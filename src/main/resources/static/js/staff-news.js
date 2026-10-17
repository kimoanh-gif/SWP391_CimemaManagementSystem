(() => {
  const form = document.getElementById("newsForm");
  const list = document.getElementById("newsList");
  const listMessage = document.getElementById("newsListMessage");
  const feedback = document.getElementById("newsFeedback");
  const cancelEdit = document.getElementById("cancelEdit");
  const csrfToken = document.querySelector('meta[name="csrf-token"]')?.content;
  const statusLabels = {
    DRAFT: "Bản nháp",
    PENDING: "Chờ duyệt",
    REJECTED: "Bị từ chối",
    PUBLISHED: "Đã đăng"
  };
  let editingId = null;
  let articles = [];

  async function request(url, options = {}) {
    const headers = new Headers(options.headers || {});
    if (options.body) headers.set("Content-Type", "application/json");
    if (csrfToken) headers.set("X-XSRF-TOKEN", csrfToken);
    const response = await fetch(url, { ...options, headers });
    if (!response.ok) {
      const body = await response.json().catch(() => ({}));
      throw new Error(body.detail || body.message || body.error || `Yêu cầu thất bại (${response.status}).`);
    }
    return response.status === 204 ? null : response.json();
  }

  function setFeedback(message, isError = false) {
    feedback.textContent = message;
    feedback.classList.toggle("is-error", isError);
    feedback.classList.toggle("is-success", !isError && Boolean(message));
  }

  function resetForm() {
    form.reset();
    editingId = null;
    document.getElementById("editorTitle").textContent = "Bài viết mới";
    cancelEdit.hidden = true;
  }

  function renderArticles() {
    list.replaceChildren();
    if (!articles.length) {
      listMessage.textContent = "Chưa có bài viết nào.";
      return;
    }
    listMessage.textContent = "";
    articles.forEach((article) => {
      const card = document.createElement("article");
      card.className = "staff-news-item";
      const title = document.createElement("h3");
      title.textContent = article.title;
      const summary = document.createElement("p");
      summary.textContent = article.summary;
      const status = document.createElement("span");
      status.className = `staff-status status-${article.status.toLowerCase()}`;
      status.textContent = statusLabels[article.status] || article.status;
      card.append(title, summary, status);

      if (["DRAFT", "REJECTED"].includes(article.status)) {
        const edit = document.createElement("button");
        edit.className = "staff-button staff-button-secondary";
        edit.type = "button";
        edit.textContent = "Chỉnh sửa";
        edit.addEventListener("click", () => startEditing(article));
        card.append(edit);
      }
      list.append(card);
    });
  }

  function startEditing(article) {
    editingId = article.newsId;
    document.getElementById("newsTitle").value = article.title;
    document.getElementById("newsSummary").value = article.summary;
    document.getElementById("newsThumbnail").value = article.thumbnailUrl || "";
    document.getElementById("newsContent").value = article.content;
    document.getElementById("editorTitle").textContent = "Chỉnh sửa bài viết";
    cancelEdit.hidden = false;
    setFeedback("");
    form.scrollIntoView({ behavior: "smooth", block: "start" });
  }

  async function loadArticles() {
    try {
      articles = await request("/api/staff/news");
      renderArticles();
    } catch (error) {
      listMessage.textContent = error.message;
      listMessage.classList.add("is-error");
    }
  }

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const status = event.submitter?.value || "DRAFT";
    const body = {
      title: document.getElementById("newsTitle").value,
      summary: document.getElementById("newsSummary").value,
      thumbnailUrl: document.getElementById("newsThumbnail").value,
      content: document.getElementById("newsContent").value,
      status
    };
    const url = editingId === null ? "/api/staff/news" : `/api/staff/news/${editingId}`;
    try {
      const article = await request(url, {
        method: editingId === null ? "POST" : "PUT",
        body: JSON.stringify(body)
      });
      setFeedback(status === "PENDING" ? "Bài viết đã được gửi duyệt." : "Đã lưu bản nháp.");
      if (editingId === null) articles.unshift(article);
      else articles = articles.map((current) => current.newsId === article.newsId ? article : current);
      renderArticles();
      resetForm();
    } catch (error) {
      setFeedback(error.message, true);
    }
  });

  cancelEdit.addEventListener("click", () => {
    resetForm();
    setFeedback("");
  });

  loadArticles();
})();
