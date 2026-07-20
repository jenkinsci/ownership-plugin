/*
 * Dev bootstrap for ownership-plugin form testing and screenshot capture.
 *
 * Creates:
 *  - security realm users (admin, alice, bob, charlie)
 *  - matrix authorization with Manage Ownership permissions
 *  - demo folder/jobs (owned, bare, specific-rights, env)
 *  - List View with Primary Owner column
 *  - global Ownership mail settings
 *
 * Place under work/init.groovy.d/ (copied from this file) or paste into Script Console.
 * Passwords match usernames (dev only).
 *
 * For Role Strategy / Job Restrictions / Authorize Project screenshots, also run
 * 02-ownership-security-testdata.groovy after this script.
 */
import com.cloudbees.hudson.plugins.folder.Folder
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipDescription
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipPlugin
import com.synopsys.arc.jenkins.plugins.ownership.OwnershipPluginConfiguration
import com.synopsys.arc.jenkins.plugins.ownership.extensions.item_ownership_policy.DropOwnershipPolicy
import com.synopsys.arc.jenkins.plugins.ownership.jobs.JobOwnerColumn
import com.synopsys.arc.jenkins.plugins.ownership.jobs.JobOwnerHelper
import com.synopsys.arc.jenkins.plugins.ownership.nodes.NodeOwnerHelper
import com.synopsys.arc.jenkins.plugins.ownership.security.itemspecific.ItemSpecificSecurity
import com.synopsys.arc.jenkins.plugins.ownership.wrappers.OwnershipBuildWrapper
import hudson.model.FreeStyleProject
import hudson.model.Item
import hudson.model.ListView
import hudson.security.AuthorizationMatrixProperty
import hudson.security.GlobalMatrixAuthorizationStrategy
import hudson.security.HudsonPrivateSecurityRealm
import hudson.security.Permission
import hudson.tasks.Shell
import hudson.views.BuildButtonColumn
import hudson.views.JobColumn
import hudson.views.LastDurationColumn
import hudson.views.LastFailureColumn
import hudson.views.LastSuccessColumn
import hudson.views.StatusColumn
import hudson.views.WeatherColumn
import java.util.HashMap
import java.util.HashSet
import jenkins.model.Jenkins
import org.jenkinsci.plugins.ownership.config.DisplayOptions
import org.jenkinsci.plugins.ownership.config.InheritanceOptions
import org.jenkinsci.plugins.ownership.model.folders.FolderOwnershipHelper
import org.jenkinsci.plugins.ownership.util.mail.MailOptions

def jenkins = Jenkins.get()

println '=== Ownership plugin: installing integration plugins check ==='
[
    'ownership',
    'cloudbees-folder',
    'matrix-auth',
    'matrix-project',
    'role-strategy',
    'job-restrictions',
    'authorize-project',
    'security-inspector',
    'mailer',
    'token-macro',
    'workflow-cps',
].each { id ->
    def p = jenkins.pluginManager.getPlugin(id)
    println(p == null ? "  MISSING: ${id}" : "  OK: ${id} (${p.version})")
}

println '=== Creating users ==='
def realm = new HudsonPrivateSecurityRealm(false, false, null)
[
    [id: 'admin',   full: 'Admin User',   mail: 'admin@example.com'],
    [id: 'alice',   full: 'Alice Owner',  mail: 'alice@example.com'],
    [id: 'bob',     full: 'Bob Co-owner', mail: 'bob@example.com'],
    [id: 'charlie', full: 'Charlie User', mail: 'charlie@example.com'],
].each { u ->
    if (realm.getUser(u.id) == null) {
        def details = realm.createAccount(u.id, u.id)
        details.setFullName(u.full)
        def mailProp = details.getProperty(hudson.tasks.Mailer.UserProperty)
        if (mailProp == null) {
            details.addProperty(new hudson.tasks.Mailer.UserProperty(u.mail))
        }
        details.save()
        println "  created user ${u.id} / ${u.id}"
    } else {
        def existing = realm.getUser(u.id)
        if (existing.getProperty(hudson.tasks.Mailer.UserProperty) == null) {
            existing.addProperty(new hudson.tasks.Mailer.UserProperty(u.mail))
            existing.save()
        }
        println "  user ${u.id} already exists"
    }
}
jenkins.setSecurityRealm(realm)

println '=== Configuring matrix authorization ==='
def strategy = new GlobalMatrixAuthorizationStrategy()
strategy.add(Jenkins.ADMINISTER, 'admin')

def itemPerms = [
    Item.READ, Item.DISCOVER, Item.CREATE, Item.DELETE, Item.CONFIGURE,
    Item.BUILD, Item.CANCEL, Item.WORKSPACE,
]
def computerPerms = [
    hudson.model.Computer.BUILD, hudson.model.Computer.CONFIGURE,
    hudson.model.Computer.CONNECT, hudson.model.Computer.CREATE,
    hudson.model.Computer.DELETE, hudson.model.Computer.DISCONNECT,
]

['alice', 'bob'].each { user ->
    strategy.add(Jenkins.READ, user)
    itemPerms.each { strategy.add(it, user) }
    strategy.add(OwnershipPlugin.MANAGE_ITEMS_OWNERSHIP, user)
}

strategy.add(Jenkins.READ, 'charlie')
strategy.add(Item.READ, 'charlie')
strategy.add(Item.DISCOVER, 'charlie')

computerPerms.each { strategy.add(it, 'alice') }
strategy.add(OwnershipPlugin.MANAGE_SLAVES_OWNERSHIP, 'alice')

jenkins.setAuthorizationStrategy(strategy)
jenkins.save()
println '  matrix auth configured (admin=ADMINISTER, alice/bob=configure+manage ownership, charlie=read)'

println '=== Configuring Ownership global settings ==='
def mailOptions = new MailOptions(
    null, null, false,
    null, null, false,
    'ownership-admins@example.com', ';', false)
def ownershipConf = new OwnershipPluginConfiguration(
    new DropOwnershipPolicy(),
    mailOptions,
    null,
    DisplayOptions.DEFAULT,
    InheritanceOptions.DEFAULT)
OwnershipPlugin.getInstance().configure(false, null, null, ownershipConf)
println '  service owners e-mail = ownership-admins@example.com'

println '=== Creating demo items ==='
Folder folder = jenkins.getItem('demo-folder') as Folder
if (folder == null) {
    folder = jenkins.createProject(Folder, 'demo-folder')
    println '  created folder demo-folder'
}

FreeStyleProject ownedJob = jenkins.getItemByFullName('demo-folder/owned-job', FreeStyleProject)
if (ownedJob == null) {
    ownedJob = folder.createProject(FreeStyleProject, 'owned-job')
    println '  created job demo-folder/owned-job'
}

FreeStyleProject bare = jenkins.getItem('bare-job') as FreeStyleProject
if (bare == null) {
    bare = jenkins.createProject(FreeStyleProject, 'bare-job')
    println '  created job bare-job'
}

FreeStyleProject specificJob = jenkins.getItemByFullName('demo-folder/specific-rights-job', FreeStyleProject)
if (specificJob == null) {
    specificJob = folder.createProject(FreeStyleProject, 'specific-rights-job')
    println '  created job demo-folder/specific-rights-job'
}

FreeStyleProject envJob = jenkins.getItemByFullName('demo-folder/env-job', FreeStyleProject)
if (envJob == null) {
    envJob = folder.createProject(FreeStyleProject, 'env-job')
    println '  created job demo-folder/env-job'
}

FolderOwnershipHelper.setOwnership(folder,
    new OwnershipDescription(true, 'alice', ['bob'] as Collection))
JobOwnerHelper.setOwnership(ownedJob,
    new OwnershipDescription(true, 'alice', ['bob', 'charlie'] as Collection))
JobOwnerHelper.setOwnership(specificJob,
    new OwnershipDescription(true, 'alice', ['bob'] as Collection))
JobOwnerHelper.setOwnership(envJob,
    new OwnershipDescription(true, 'alice', ['bob'] as Collection))
println '  ownership set on demo-folder, owned-job, specific-rights-job, env-job'

// Item-specific security matrix (Configure specific access rights)
Map<Permission, Set<String>> matrixPerms = new HashMap<>()
matrixPerms.put(Item.READ, new HashSet<>(['charlie']))
matrixPerms.put(Item.BUILD, new HashSet<>(['charlie']))
JobOwnerHelper.setProjectSpecificSecurity(specificJob,
    new ItemSpecificSecurity(new AuthorizationMatrixProperty(matrixPerms)))
println '  item-specific security set on specific-rights-job (charlie: READ+BUILD)'

// Build wrapper + shell / token-macro example steps
if (envJob.getBuildWrappersList().get(OwnershipBuildWrapper) == null) {
    envJob.getBuildWrappersList().add(new OwnershipBuildWrapper(true, true))
}
envJob.getBuildersList().clear()
envJob.getBuildersList().add(new Shell(
    'echo "JOB_OWNER=$JOB_OWNER"\n' +
    'echo "JOB_OWNER_EMAIL=$JOB_OWNER_EMAIL"\n' +
    'echo "JOB_COOWNERS=$JOB_COOWNERS"\n' +
    'echo "Token macro example: \\${OWNERSHIP,var=JOB_OWNER}"'))
envJob.save()
println '  env-job: OwnershipBuildWrapper + shell build step configured'

try {
    def builtIn = jenkins.getComputer('')?.node
    if (builtIn != null) {
        NodeOwnerHelper.setOwnership(builtIn,
            new OwnershipDescription(true, 'admin', ['alice'] as Collection))
        println '  ownership set on built-in node (admin/alice)'
    }
} catch (Exception e) {
    println "  skip node ownership: ${e.message}"
}

println '=== Creating List View with Primary Owner column ==='
def viewName = 'All (with Ownership)'
def existingView = jenkins.getView(viewName)
if (existingView != null) {
    jenkins.deleteView(existingView)
}
def listView = new ListView(viewName)
listView.setIncludeRegex('.*')
listView.getColumns().clear()
listView.getColumns().add(new StatusColumn())
listView.getColumns().add(new WeatherColumn())
listView.getColumns().add(new JobColumn())
listView.getColumns().add(new JobOwnerColumn())
listView.getColumns().add(new LastSuccessColumn())
listView.getColumns().add(new LastFailureColumn())
listView.getColumns().add(new LastDurationColumn())
listView.getColumns().add(new BuildButtonColumn())
jenkins.addView(listView)
println "  view '${viewName}' created"

jenkins.save()
println '=== Done. Log in at /jenkins/ as admin/admin, alice/alice, bob/bob, charlie/charlie ==='
println 'Demo items:'
println '  - bare-job (no ownership)'
println '  - demo-folder (alice/bob)'
println '  - demo-folder/owned-job (alice/bob/charlie)'
println '  - demo-folder/specific-rights-job (owners + item-specific matrix)'
println '  - demo-folder/env-job (build wrapper + shell)'
println '  - view: All (with Ownership)'
println 'Screenshot URLs (base /jenkins):'
println '  - /job/demo-folder/job/owned-job/'
println '  - /job/demo-folder/job/owned-job/ownership/'
println '  - /job/demo-folder/job/owned-job/ownership/manage-owners'
println '  - /job/demo-folder/job/specific-rights-job/ownership/configure-project-specifics'
println '  - /view/All%20(with%20Ownership)/'
println '  - /manage/configure  (Ownership section)'
println 'Then run: cd dev/screenshots && npm ci && npm run capture'
println 'For security screenshots, run 02-ownership-security-testdata.groovy first.'
