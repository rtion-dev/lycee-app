let pendingTab = location.hash.slice(1);

function openRequestedTab() {
  if (!["classes", "applications"].includes(pendingTab)) return;

  const dashboard = document.getElementById("dashboard");
  const button = document.querySelector(
    `[data-tab="${pendingTab}"]`
  );

  if (!dashboard || dashboard.hidden || !button) return;

  // نستهلك الطلب مرة واحدة، قبل تشغيل الزر.
  pendingTab = "";

  if (button.getAttribute("aria-current") !== "page") {
    button.click();
  }
}

new MutationObserver(openRequestedTab).observe(document.body, {
  subtree: true,
  attributes: true,
  attributeFilter: ["hidden"]
});

// نحدّث الرابط حسب الزر اللي اختاره المستخدم.
document.addEventListener("click", (event) => {
  const button = event.target.closest("button[data-tab]");
  if (!button) return;

  const tab = button.dataset.tab;
  if (!["classes", "applications"].includes(tab)) return;

  pendingTab = "";
  history.replaceState(null, "", `#${tab}`);
});

window.addEventListener("hashchange", () => {
  pendingTab = location.hash.slice(1);
  openRequestedTab();
});

openRequestedTab();