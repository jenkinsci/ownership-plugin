/**
 * Persist collapsed/expanded state for ownership summary cards.
 * Default: collapsed when data-default-collapsed="true".
 */
(function () {
  function storageKey(el) {
    return "ownership-summary:" + (el.getAttribute("data-storage-key") || "default");
  }

  function applyStoredState(details, el) {
    try {
      var stored = window.localStorage.getItem(storageKey(el));
      if (stored === "open") {
        details.open = true;
      } else if (stored === "closed") {
        details.open = false;
      } else {
        details.open = el.getAttribute("data-default-collapsed") !== "true";
      }
    } catch (e) {
      details.open = el.getAttribute("data-default-collapsed") !== "true";
    }
  }

  function bind(el) {
    var details = el.tagName === "DETAILS" ? el : el.querySelector("details");
    if (!details || details.getAttribute("data-ownership-bound") === "true") {
      return;
    }
    details.setAttribute("data-ownership-bound", "true");
    applyStoredState(details, el);
    details.addEventListener("toggle", function () {
      try {
        window.localStorage.setItem(storageKey(el), details.open ? "open" : "closed");
      } catch (e) {
        /* ignore quota / private mode */
      }
    });
  }

  function init() {
    document.querySelectorAll("[data-ownership-summary]").forEach(bind);
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();
