// Renders the user's HashMap-based frequency table and LinkedList-based recent log.

(async () => {
    const freqBody   = document.querySelector('#freqTable tbody');
    const recentBody = document.querySelector('#recentTable tbody');

    try {
        const data = await api.get('/api/history');
        const freq = data.frequencies || {};
        const rows = Object.entries(freq).sort((a, b) => b[1] - a[1]);
        freqBody.innerHTML = rows.length
            ? rows.map(([k, v]) => `<tr><td>${k}</td><td>${v}</td></tr>`).join('')
            : '<tr><td class="muted" colspan="2">No searches yet — try one!</td></tr>';

        const recent = data.recent || [];
        recentBody.innerHTML = recent.length
            ? recent.map(r => {
                const ts = r.timestamp ? new Date(Number(r.timestamp)).toLocaleString() : '';
                return `<tr><td>${ts}</td><td>${r.type || ''}</td><td>${r.q || ''}</td><td>${r.resultsCount ?? ''}</td></tr>`;
              }).join('')
            : '<tr><td class="muted" colspan="4">No searches yet.</td></tr>';
    } catch (e) {
        freqBody.innerHTML   = `<tr><td class="muted" colspan="2">Sign in to view your history (FULL mode). Error: ${e.message}</td></tr>`;
        recentBody.innerHTML = `<tr><td class="muted" colspan="4">Sign in to view your history.</td></tr>`;
    }
})();
