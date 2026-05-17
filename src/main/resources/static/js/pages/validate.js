document.getElementById('form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const type  = document.getElementById('type').value;
    const value = document.getElementById('value').value;
    const out   = document.getElementById('out');
    try {
        const data = await api.post('/api/validate', { type, value });
        out.innerHTML = `<div class="validate-out ${data.valid ? 'valid' : 'invalid'}">
            ${data.valid ? '✅' : '❌'}
            <span><b>${escapeHtml(data.value)}</b> — ${escapeHtml(data.message)}</span>
        </div>`;
    } catch (e) {
        out.innerHTML = `<div class="validate-out invalid">Error: ${escapeHtml(e.message)}</div>`;
    }
});
