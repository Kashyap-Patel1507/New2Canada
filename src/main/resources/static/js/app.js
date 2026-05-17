// Shared helpers used by every page.

window.api = {
    async get(path) {
        const headers = await authHeaders();
        const r = await fetch(path, { headers });
        if (!r.ok) throw new Error(r.statusText);
        return await r.json();
    },
    async post(path, formData) {
        const headers = await authHeaders();
        headers['Content-Type'] = 'application/x-www-form-urlencoded';
        const body = new URLSearchParams(formData).toString();
        const r = await fetch(path, { method: 'POST', headers, body });
        if (!r.ok) throw new Error(r.statusText);
        return await r.json();
    }
};

async function authHeaders() {
    const headers = {};
    if (window.getIdToken) {
        const token = await window.getIdToken();
        if (token) headers['Authorization'] = 'Bearer ' + token;
    }
    return headers;
}

// Demo-mode banner toggler. Injects the banner text so every page can just
// drop in an empty <div id="demoBanner"> and we'll fill it.
async function checkDemoBanner() {
    try {
        const stats = await fetch('/api/debug').then(r => r.json());
        const banner = document.getElementById('demoBanner');
        if (banner && stats.demoMode) {
            if (!banner.innerHTML.trim()) {
                banner.innerHTML =
                    'Running in <b>DEMO mode</b>. Auth & cloud persistence are disabled — ' +
                    'add <code>serviceAccountKey.json</code> and your Firebase web config ' +
                    '(see <code>README.md</code>) to enable FULL mode.';
            }
            banner.style.display = 'block';
        }
    } catch (e) { /* server warming up */ }
}

// ---- Result card renderer ---------------------------------------------------
function renderResult(item) {
    const fields = item.fields || {};
    const tags = [];
    if (item.source)         tags.push(`<span class="tag accent">${escape(item.source)}</span>`);
    if (fields.city)         tags.push(`<span class="tag">📍 ${escape(fields.city)}</span>`);
    if (fields.bedrooms !== undefined) tags.push(`<span class="tag">🛏 ${fields.bedrooms} BR</span>`);
    if (fields.bank)         tags.push(`<span class="tag brand">${escape(fields.bank)}</span>`);
    if (fields.carrier)      tags.push(`<span class="tag brand">${escape(fields.carrier)}</span>`);
    if (fields.dataGb)       tags.push(`<span class="tag">📊 ${fields.dataGb} GB</span>`);
    if (fields.employer)     tags.push(`<span class="tag">🏢 ${escape(fields.employer)}</span>`);
    if (fields.hourlyRate)   tags.push(`<span class="tag ok">$${fields.hourlyRate}/h</span>`);
    if (fields.price)        tags.push(`<span class="tag ok">$${fields.price}/mo</span>`);
    if (fields.monthlyPrice) tags.push(`<span class="tag ok">$${fields.monthlyPrice}/mo</span>`);
    if (fields.monthlyFee !== undefined) {
        tags.push(fields.monthlyFee === 0
            ? `<span class="tag ok">$0 fee</span>`
            : `<span class="tag ok">$${fields.monthlyFee}/mo fee</span>`);
    }
    if (fields.amount)       tags.push(`<span class="tag ok">$${fields.amount}</span>`);
    if (fields.level)        tags.push(`<span class="tag">🎓 ${escape(fields.level)}</span>`);
    if (fields.provider)     tags.push(`<span class="tag">🏛 ${escape(fields.provider)}</span>`);
    if (fields.studentEligible) tags.push(`<span class="tag ok">Student-friendly</span>`);
    if (fields.internationalEligible) tags.push(`<span class="tag ok">International OK</span>`);

    const safeUrl = item.url && item.url.startsWith('http') ? item.url : '#';
    return `
        <article class="result-card">
            <h3><a href="${safeUrl}" target="_blank" rel="noopener noreferrer">${escape(item.title || 'Untitled')}</a></h3>
            <div class="subtitle">${escape(item.subtitle || '')}</div>
            <div class="tags">${tags.join('')}</div>
        </article>`;
}

function escape(s) {
    return String(s)
        .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

function skeletonList(n) {
    return Array.from({ length: n }, () => `
        <div class="skeleton-card">
            <div class="skeleton skeleton-line" style="width:60%;height:18px"></div>
            <div class="skeleton skeleton-line" style="width:40%;height:13px"></div>
            <div class="skeleton skeleton-line" style="width:25%;height:13px"></div>
        </div>`).join('');
}

function qsParam(name) {
    return new URLSearchParams(window.location.search).get(name) || '';
}

window.renderResult = renderResult;
window.skeletonList = skeletonList;
window.qsParam      = qsParam;
window.escapeHtml   = escape;

document.addEventListener('DOMContentLoaded', checkDemoBanner);
