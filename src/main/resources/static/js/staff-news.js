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
  let editingId = null;

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const submitter = event.submitter;
    if (!submitter || !fields.title.value.trim() || !fields.content.value.trim()) {
      feedback.textContent = "Enter a title and article content.";
      return;
    }
    const payload = {
      title: fields.title.value.trim(),
      thumbnailUrl: fields.thumbnailUrl.value.trim() || null,
      content: fields.content.value.trim(),
      status: submitter.value
    };
    const method = editingId == null ? "POST" : "PUT";
    const path = editingId == null ? "/api/staff/news" : `/api/staff/news/${editingId}`;
    feedback.textContent = "Saving article...";
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
        feedback.textContent = body?.detail || body?.message || "Unable to save the article.";
        return;
      }
      feedback.textContent = payload.status === "DRAFT" ? "Draft saved." : "Article submitted for review.";
      clearEditor();
      await loadArticles();
    } catch (error) {
      feedback.textContent = "Unable to connect. Please try again.";
    }
  });

  cancelEdit.addEventListener("click", clearEditor);
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
    const card = document.createElement("article");
    card.className = "staff-news-item";
    const title = document.createElement("h3");
    title.textContent = article.title || "Article";
    const excerpt = document.createElement("p");
    const content = article.content || "";
    excerpt.textContent = content.length > 140 ? `${content.slice(0, 140)}...` : content;
    const status = document.createElement("span");
    status.className = `staff-status status-${String(article.status || "").toLowerCase()}`;
    status.textContent = article.status || "";
    card.append(title, excerpt, status);
    if (["DRAFT", "REJECTED"].includes(String(article.status).toUpperCase())) {
      const button = document.createElement("button");
      button.type = "button";
      button.className = "staff-button";
      button.textContent = "Edit";
      button.addEventListener("click", () => beginEdit(article));
      card.append(button);
    }
    return card;
  }

  function beginEdit(article) {
    editingId = article.newsId;
    fields.title.value = article.title || "";
    fields.thumbnailUrl.value = article.thumbnailUrl || "";
    fields.content.value = article.content || "";
    document.getElementById("editorTitle").textContent = "Edit Article";
    cancelEdit.hidden = false;
    feedback.textContent = "";
    fields.title.focus();
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function clearEditor() {
    editingId = null;
    form.reset();
    document.getElementById("editorTitle").textContent = "New Article";
    cancelEdit.hidden = true;
    feedback.textContent = "";
  }
})();
