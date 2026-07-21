/*
 * Optional security-integration bootstrap for screenshot capture.
 *
 * Prerequisites: run 01-ownership-testdata.groovy first (users + demo items).
 *
 * Switches authorization to Role Strategy with ownership macros, configures
 * Job Restrictions on built-in + demo-agent, and Authorize Project strategies.
 *
 * WARNING: replaces Global Matrix Authorization from script 01.
 * Paste into Script Console or place under work/init.groovy.d/ (after 01).
 */
import com.michelin.cio.hudson.plugins.rolestrategy.AuthorizationType
import com.michelin.cio.hudson.plugins.rolestrategy.PermissionEntry
import com.michelin.cio.hudson.plugins.rolestrategy.Role
import com.michelin.cio.hudson.plugins.rolestrategy.RoleBasedAuthorizationStrategy
import com.michelin.cio.hudson.plugins.rolestrategy.RoleMap
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipPlugin
import com.synopsys.arc.jenkins.plugins.ownership.nodes.NodeOwnerHelper
import com.synopsys.arc.jenkins.plugins.ownership.security.authorizeproject.OwnershipAuthorizeProjectStrategy
import com.synopsys.arc.jenkins.plugins.ownership.security.jobrestrictions.OwnersListJobRestriction
import com.synopsys.arc.jenkins.plugins.ownership.util.ui.UserSelector
import com.synopsys.arc.jenkins.plugins.rolestrategy.RoleType
import com.synopsys.arc.jenkinsci.plugins.jobrestrictions.nodes.JobRestrictionProperty
import hudson.ExtensionList
import hudson.model.Computer
import hudson.model.Item
import hudson.model.Node
import hudson.model.Run
import hudson.security.Permission
import hudson.slaves.DumbSlave
import hudson.slaves.JNLPLauncher
import hudson.slaves.RetentionStrategy
import java.lang.reflect.Constructor
import java.util.TreeMap
import java.util.TreeSet
import jenkins.model.Jenkins
import jenkins.security.QueueItemAuthenticatorConfiguration
import org.jenkinsci.plugins.authorizeproject.GlobalQueueItemAuthenticator
import org.jenkinsci.plugins.authorizeproject.ProjectQueueItemAuthenticator
import org.jenkinsci.plugins.authorizeproject.strategy.AnonymousAuthorizationStrategy
import org.jenkinsci.plugins.authorizeproject.strategy.TriggeringUsersAuthorizationStrategy

def jenkins = Jenkins.get()

println '=== 02: Role Strategy + Job Restrictions + Authorize Project ==='

def createRole = { String name, String pattern, Set<Permission> permissions ->
    Constructor<Role> c = Role.class.getDeclaredConstructor(String, String, Set)
    c.setAccessible(true)
    try {
        return c.newInstance(name, pattern, permissions)
    } finally {
        c.setAccessible(false)
    }
}

def createRoleMap = { SortedMap<Role, Set<PermissionEntry>> granted ->
    Constructor<RoleMap> c = RoleMap.class.getDeclaredConstructor(SortedMap)
    c.setAccessible(true)
    try {
        return c.newInstance(granted)
    } finally {
        c.setAccessible(false)
    }
}

def eitherSid = { String sid ->
    def s = new TreeSet()
    s.add(new PermissionEntry(AuthorizationType.EITHER, sid))
    return s
}

def userSid = { String sid ->
    def s = new TreeSet()
    s.add(PermissionEntry.user(sid))
    return s
}

// --- Global roles ---
Set<Permission> adminPerms = new HashSet<>()
adminPerms.add(Jenkins.ADMINISTER)
Role adminRole = createRole('admin', '.*', adminPerms)

Set<Permission> authPerms = new HashSet<>()
authPerms.add(Jenkins.READ)
authPerms.add(hudson.model.View.READ)
Role authenticatedRole = createRole('authenticated', '.*', authPerms)

SortedMap<Role, Set<PermissionEntry>> globalGranted = new TreeMap<>()
globalGranted.put(adminRole, userSid('admin'))
globalGranted.put(authenticatedRole, eitherSid('authenticated'))
RoleMap globalMap = createRoleMap(globalGranted)

// --- Item roles with ownership macros ---
Set<Permission> ownerPerms = new HashSet<>()
ownerPerms.add(OwnershipPlugin.MANAGE_ITEMS_OWNERSHIP)
ownerPerms.addAll(Item.PERMISSIONS.getPermissions())
ownerPerms.addAll(Run.PERMISSIONS.getPermissions())
Role ownerRole = createRole('@Owner', '.*', ownerPerms)

Set<Permission> coOwnerPerms = new HashSet<>()
coOwnerPerms.addAll(Item.PERMISSIONS.getPermissions())
coOwnerPerms.addAll(Run.PERMISSIONS.getPermissions())
coOwnerPerms.remove(Item.DELETE)
coOwnerPerms.remove(Run.DELETE)
Role coOwnerRole = createRole('@CoOwner', '.*', coOwnerPerms)

SortedMap<Role, Set<PermissionEntry>> itemGranted = new TreeMap<>()
itemGranted.put(ownerRole, eitherSid('authenticated'))
itemGranted.put(coOwnerRole, eitherSid('authenticated'))
RoleMap itemMap = createRoleMap(itemGranted)

// --- Node roles ---
Set<Permission> nodeOwnerPerms = new HashSet<>()
nodeOwnerPerms.add(OwnershipPlugin.MANAGE_SLAVES_OWNERSHIP)
nodeOwnerPerms.addAll(Computer.PERMISSIONS.getPermissions())
Role nodeOwnerRole = createRole('@Owner', '.*', nodeOwnerPerms)

Set<Permission> nodeCoOwnerPerms = new HashSet<>()
nodeCoOwnerPerms.addAll(Computer.PERMISSIONS.getPermissions())
nodeCoOwnerPerms.remove(Computer.DELETE)
nodeCoOwnerPerms.remove(Computer.CONFIGURE)
Role nodeCoOwnerRole = createRole('@CoOwner', '.*', nodeCoOwnerPerms)

SortedMap<Role, Set<PermissionEntry>> nodeGranted = new TreeMap<>()
nodeGranted.put(nodeOwnerRole, eitherSid('authenticated'))
nodeGranted.put(nodeCoOwnerRole, eitherSid('authenticated'))
RoleMap nodeMap = createRoleMap(nodeGranted)

Map<String, RoleMap> grantedRoles = new HashMap<>()
grantedRoles.put(RoleType.Global.getStringType(), globalMap)
grantedRoles.put(RoleType.Project.getStringType(), itemMap)
grantedRoles.put(RoleType.Slave.getStringType(), nodeMap)

jenkins.setAuthorizationStrategy(new RoleBasedAuthorizationStrategy(grantedRoles))
jenkins.save()
println '  Role Strategy configured (admin, authenticated, @Owner/@CoOwner item+node roles)'

// --- Demo agent for common-node Job Restrictions screenshot ---
DumbSlave demoAgent = jenkins.getNode('demo-agent') as DumbSlave
if (demoAgent == null) {
    demoAgent = new DumbSlave(
        'demo-agent',
        'Demo agent for ownership Job Restrictions screenshots',
        '/tmp/demo-agent',
        '1',
        Node.Mode.NORMAL,
        'demo',
        new JNLPLauncher(),
        RetentionStrategy.NOOP,
        [])
    jenkins.addNode(demoAgent)
    println '  created node demo-agent'
}
NodeOwnerHelper.setOwnership(demoAgent,
    new OwnershipDescription(true, 'alice', ['bob'] as Collection))

def ensureOwnersRestriction = { Node node, String ownerId, boolean acceptCoOwners ->
    def props = node.getNodeProperties()
    def existing = props.get(JobRestrictionProperty)
    if (existing != null) {
        props.remove(existing)
    }
    props.add(new JobRestrictionProperty(
        new OwnersListJobRestriction([new UserSelector(ownerId)], acceptCoOwners)))
    node.save()
}

try {
    def builtIn = jenkins.getComputer('')?.node
    if (builtIn != null) {
        ensureOwnersRestriction(builtIn, 'alice', true)
        println '  Job Restrictions (Owners list) on built-in'
    }
} catch (Exception e) {
    println "  skip built-in restrictions: ${e.message}"
}

ensureOwnersRestriction(demoAgent, 'alice', false)
println '  Job Restrictions (Owners list) on demo-agent'

// --- Authorize Project ---
try {
    def ownershipStrategyId = Jenkins.get().getDescriptor(OwnershipAuthorizeProjectStrategy)?.id
    def triggeringId = Jenkins.get().getDescriptor(TriggeringUsersAuthorizationStrategy)?.id
    def anonymousId = Jenkins.get().getDescriptor(AnonymousAuthorizationStrategy)?.id

    Map<String, Boolean> enabled = new LinkedHashMap<>()
    if (ownershipStrategyId != null) {
        enabled.put(ownershipStrategyId, true)
    }
    if (triggeringId != null) {
        enabled.put(triggeringId, true)
    }
    if (anonymousId != null) {
        enabled.put(anonymousId, true)
    }

    def qiac = ExtensionList.lookupSingleton(QueueItemAuthenticatorConfiguration)
    def authenticators = qiac.getAuthenticators()
    authenticators.replaceBy([
        new ProjectQueueItemAuthenticator(enabled),
        new GlobalQueueItemAuthenticator(new OwnershipAuthorizeProjectStrategy()),
    ])
    jenkins.save()
    println '  Authorize Project: ProjectQueueItemAuthenticator + Global Run as Job Owner'
} catch (Exception e) {
    println "  skip Authorize Project config: ${e.message}"
}

println '=== 02 Done. Capture security shots with SHOT_FILTER=security ==='
println '  - /manage/role-strategy/'
println '  - /manage/role-strategy/assign-roles'
println '  - /computer/(built-in)/configure'
println '  - /computer/demo-agent/configure'
println '  - /manage/configureSecurity/  (Access Control for Builds)'
