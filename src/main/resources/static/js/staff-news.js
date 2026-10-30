(() => {
  const form = document.getElementById("newsForm");
  const list = document.getElementById("newsList");
  const feedback = document.getElementById("newsFeedback");
  const listMessage = document.getElementById("newsListMessage");
  const cancelEdit = document.getElementById("cancelEdit");
  if (!form || !list || !feedback || !listMessage || !cancelEdit) return;

  const csrf = document.querySelector('meta[name="csrf-token"]')?.content;
  const fields = {
    title: document.getElementById("newsTitle"),
    thumbnailUrl: document.getElementById("newsThumbnail"),
    content: document.getElementById("newsContent")
  };
  const statusLabels = { DRAFT: "Draft", PENDING: "Pending Review", APPROVED: "Approved", PUBLISHED: "Published", REJECTED: "Rejected" };
  let editingId = null;

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const submitter = event.submitter;
    if (!submitter || !fields.title.value.trim() || !fields.content.value.trim()) {
      showFeedback("Enter a title and article content.", "error");
      return;
    }
    // A thumbnail is needed only to submit for approval; drafts can be saved without one.
    if (submitter.value === "PENDING" && !fields.thumbnailUrl.value.trim()) {
      fields.thumbnailUrl.setAttribute("aria-invalid", "true");
      fields.thumbnailUrl.focus();
      showFeedback("Add a thumbnail image before submitting for approval.", "error");
      return;
    }
    fields.thumbnailUrl.removeAttribute("aria-invalid");
    const payload = {
      title: fields.title.value.trim(),
      thumbnailUrl: fields.thumbnailUrl.value.trim() || null,
      content: fields.content.value.trim(),
      status: submitter.value
    };
    const method = editingId == null ? "POST" : "PUT";
    const path = editingId == null ? "/api/staff/news" : `/api/staff/news/${editingId}`;
    const buttons = [...form.querySelectorAll("button")];
    buttons.forEach((button) => { button.disabled = true; });
    showFeedback("Saving article...", "info");
    try {
      const response = await fetch(path, {
        method,
        headers: {
          "Content-Type": "application/json",
          Accept: "application/json",
          ...(csrf ? { "X-CSRF-TOKEN": csrf } : {})
        },
        body: JSON.stringify(payload)
      });
      const body = await response.json().catch(() => null);
      if (!response.ok) {
        showFeedback(body?.message || body?.detail || "Unable to save the article.", "error");
        return;
      }
      clearEditor();
      showFeedback(payload.status === "DRAFT" ? "Draft saved." : "Article submitted for manager review.", "success");
      await loadArticles();
    } catch (error) {
      showFeedback("Unable to connect. Please try again.", "error");
    } finally {
      buttons.forEach((button) => { button.disabled = false; });
    }
  });

  fields.thumbnailUrl.addEventListener("input", () => fields.thumbnailUrl.removeAttribute("aria-invalid"));

  cancelEdit.addEventListener("click", () => {
    clearEditor();
    hideFeedback();
  });
  loadArticles();

  async function loadArticles() {
    listMessage.textContent = "Loading articles...";
    try {
      const response = await fetch("/api/staff/news", { headers: { Accept: "application/json" } });
      const articles = await response.json().catch(() => null);
      if (!response.ok || !Array.isArray(articles)) {
        listMessage.textContent = "Unable to load articles.";
        return;
      }
      list.replaceChildren();
      listMessage.textContent = articles.length ? "" : "No articles yet.";
      articles.forEach((article) => list.append(createArticleCard(article)));
    } catch (error) {
      listMessage.textContent = "Unable to connect while loading articles.";
    }
  }

  function createArticleCard(article) {
    const status = String(article.status || "").toUpperCase();
    const card = el("article", "news-item");

    const head = el("div", "news-item-head");
    head.append(el("h3", "", article.title || "Article"),
      el("span", `badge badge-${status.toLowerCase()} news-status`, statusLabels[status] || status));
    card.append(head);

    const content = article.content || "";
    card.append(el("p", "", content.length > 140 ? `${content.slice(0, 140)}...` : content));

    const foot = el("div", "news-item-foot");
    foot.append(el("span", "", formatDate(article.updatedAt || article.createdAt)));
    if (status === "DRAFT" || status === "REJECTED") {
      const button = el("button", "btn btn-secondary", "Edit");
      button.type = "button";
      button.addEventListener("click", () => beginEdit(article));
      foot.append(button);
    }
    card.append(foot);
    return card;
  }

  function beginEdit(article) {
    editingId = article.newsId;
    fields.title.value = article.title || "";
    fields.thumbnailUrl.value = article.thumbnailUrl || "";
    fields.content.value = article.content || "";
    document.getElementById("editorTitle").textContent = "Edit Article";
    cancelEdit.hidden = false;
    hideFeedback();
    fields.title.focus();
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function clearEditor() {
    editingId = null;
    form.reset();
    fields.thumbnailUrl.removeAttribute("aria-invalid");
    document.getElementById("editorTitle").textContent = "New Article";
    cancelEdit.hidden = true;
  }

  function showFeedback(text, type) {
    feedback.textContent = text;
    feedback.className = `alert alert-${type}`;
    feedback.hidden = false;
  }

  function hideFeedback() {
    feedback.textContent = "";
    feedback.hidden = true;
  }

  function formatDate(value) {
    if (!value) return "";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return "";
    return `Updated ${date.toLocaleString("en-GB", { day: "2-digit", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit" })}`;
  }

  function el(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text != null) node.textContent = text;
    return node;
  }
})();
