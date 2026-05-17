document.getElementById('form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const word = document.getElementById('word').value.trim();
    const out  = document.getElementById('out');
    if (!word) { out.innerHTML = ''; return; }
    try {
        const data = await api.get('/api/spellcheck?word=' + encodeURIComponent(word));
        if (data.valid) {
            out.innerHTML = `<div class="validate-out valid">✅ "<b>${escapeHtml(data.word)}</b>" is spelled correctly.</div>`;
            return;
        }
        const suggestions = data.suggestions || [];
        if (!suggestions.length) {
            out.innerHTML = `<div class="validate-out invalid">❌ No close matches found for "<b>${escapeHtml(data.word)}</b>".</div>`;
            return;
        }
        const list = suggestions.map(s =>
            `<li><a href="/apartments.html?q=${encodeURIComponent(s)}">${escapeHtml(s)}</a></li>`).join('');
        out.innerHTML = `
            <div class="did-you-mean">Did you mean <b>${escapeHtml(data.corrected)}</b>?</div>
            <div class="card">
                <h3>Top suggestions</h3>
                <p class="muted">Sorted by edit distance ascending, then corpus frequency descending.</p>
                <ol>${list}</ol>
            </div>`;
    } catch (e) {
        out.innerHTML = `<div class="validate-out invalid">Error: ${escapeHtml(e.message)}</div>`;
    }
});
