// Landing-page logic: live stats, hero search, hero autocomplete.

// ----- Live stats -----------------------------------------------------------
(async () => {
    try {
        const s = await fetch('/api/debug').then(r => r.json());
        document.getElementById('statDocs').textContent    = s.totalDocs;
        document.getElementById('statTerms').textContent   = s.trieSize;
        document.getElementById('statDict').textContent    = s.dictionarySize;
        document.getElementById('statQueries').textContent = s.totalQueries;
    } catch (e) { /* server still warming up */ }
})();

// ----- Hero autocomplete ----------------------------------------------------
const q  = document.getElementById('heroQ');
const ac = document.getElementById('heroAc');
let acTimer;
q.addEventListener('input', () => {
    clearTimeout(acTimer);
    acTimer = setTimeout(async () => {
        const term = q.value.trim();
        if (term.length < 2) { ac.style.display = 'none'; return; }
        try {
            const data = await fetch('/api/autocomplete?q=' + encodeURIComponent(term))
                .then(r => r.json());
            const words = data.suggestions || [];
            if (!words.length) { ac.style.display = 'none'; return; }
            ac.innerHTML = `<div class="py-2">` + words.map(w => `
                <div class="ac-item">
                    <span class="material-symbols-outlined text-outline text-sm">location_on</span>
                    <span>${w}</span>
                </div>`).join('') + `</div>`;
            ac.style.display = 'block';
            [...ac.querySelectorAll('.ac-item')].forEach(el => el.onclick = () => {
                q.value = el.querySelector('span:last-child').textContent;
                ac.style.display = 'none';
            });
        } catch (e) { ac.style.display = 'none'; }
    }, 120);
});
document.addEventListener('click', e => {
    if (!ac.contains(e.target) && e.target !== q) ac.style.display = 'none';
});

// ----- Hero submit redirects to the right page ------------------------------
document.getElementById('heroSearch').addEventListener('submit', (e) => {
    e.preventDefault();
    const query = q.value.trim();
    const url   = '/apartments.html' + (query ? `?q=${encodeURIComponent(query)}` : '');
    window.location = url;
});
