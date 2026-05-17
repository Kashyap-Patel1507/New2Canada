const input = document.getElementById('prefix');
const list  = document.getElementById('list');

let timer = null;
input.addEventListener('input', () => {
    clearTimeout(timer);
    timer = setTimeout(async () => {
        const term = input.value.trim();
        if (term.length < 1) { list.innerHTML = '<li class="muted">Type something above</li>'; return; }
        try {
            const data = await api.get('/api/autocomplete?q=' + encodeURIComponent(term) + '&limit=15');
            list.innerHTML = (data.suggestions || []).length
                ? data.suggestions.map(s => `<li>${s}</li>`).join('')
                : '<li class="muted">No completions yet — the live crawl is still warming up.</li>';
        } catch (e) {
            list.innerHTML = '<li class="muted">Error: ' + e.message + '</li>';
        }
    }, 100);
});
