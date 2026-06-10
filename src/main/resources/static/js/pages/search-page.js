// Shared logic for /apartments.html.
// The page sets window.PAGE_TYPE before this script loads.

const TYPE     = window.PAGE_TYPE || 'apartments';
const q        = document.getElementById('q');
const acList   = document.getElementById('acList');
const results  = document.getElementById('results');
const dym      = document.getElementById('didYouMean');
const countEl  = document.getElementById('resultCount');
const provSel  = document.getElementById('provinceFilter');   // may be absent
const citySel  = document.getElementById('cityFilter');       // may be absent
const clearBtn = document.getElementById('clearFilters');     // may be absent

const HAS_FILTERS = provSel && citySel;

// Pre-fill from ?q=… / ?city=… / ?province=…
const initialQ        = qsParam('q');
const initialCity     = qsParam('city');
const initialProvince = qsParam('province');
if (initialQ) q.value = initialQ;

let citiesByProvince = {};

// All 13 Canadian provinces & territories, plus their major cities — always
// offered in the dropdowns, even before any listings have been crawled for
// them. Crawled cities (from /api/facets) are merged in on top of this.
const ALL_CITIES_BY_PROVINCE = {
    AB: ['Calgary', 'Edmonton', 'Red Deer', 'Lethbridge', 'St. Albert', 'Medicine Hat', 'Grande Prairie', 'Airdrie', 'Spruce Grove', 'Leduc', 'Fort McMurray'],
    BC: ['Vancouver', 'Victoria', 'Surrey', 'Burnaby', 'Richmond', 'Abbotsford', 'Kelowna', 'Kamloops', 'Nanaimo', 'Prince George', 'Coquitlam', 'Langley'],
    MB: ['Winnipeg', 'Brandon', 'Steinbach', 'Thompson', 'Portage la Prairie'],
    NB: ['Saint John', 'Moncton', 'Fredericton', 'Dieppe', 'Miramichi'],
    NL: ["St. John's", 'Mount Pearl', 'Corner Brook', 'Conception Bay South'],
    NS: ['Halifax', 'Sydney', 'Dartmouth', 'Truro', 'New Glasgow'],
    NT: ['Yellowknife', 'Hay River', 'Inuvik'],
    NU: ['Iqaluit', 'Rankin Inlet', 'Arviat'],
    ON: ['Toronto', 'Ottawa', 'Mississauga', 'Brampton', 'Hamilton', 'London', 'Markham', 'Vaughan', 'Kitchener', 'Windsor', 'Richmond Hill', 'Oakville', 'Burlington', 'Oshawa', 'Barrie', 'St. Catharines', 'Guelph', 'Cambridge', 'Whitby', 'Kingston', 'Waterloo', 'Sudbury', 'Thunder Bay', 'Niagara Falls'],
    PE: ['Charlottetown', 'Summerside', 'Stratford', 'Cornwall'],
    QC: ['Montreal', 'Quebec City', 'Laval', 'Gatineau', 'Longueuil', 'Sherbrooke', 'Saguenay', 'Levis', 'Trois-Rivieres', 'Terrebonne', 'Saint-Jean-sur-Richelieu', 'Repentigny', 'Brossard'],
    SK: ['Saskatoon', 'Regina', 'Prince Albert', 'Moose Jaw', 'Swift Current', 'Yorkton'],
    YT: ['Whitehorse', 'Dawson City', 'Watson Lake'],
};
const ALL_PROVINCES = Object.keys(ALL_CITIES_BY_PROVINCE);

// ----- Filter dropdowns -----------------------------------------------------
async function loadFacets() {
    if (!HAS_FILTERS) return;
    try {
        const data = await api.get('/api/facets?type=' + TYPE);
        const crawledCitiesByProvince = data.citiesByProvince || {};

        // Merge crawled cities into the static list so nothing is missed
        // in either direction.
        citiesByProvince = {};
        for (const prov of ALL_PROVINCES) {
            citiesByProvince[prov] = Array.from(new Set([
                ...(ALL_CITIES_BY_PROVINCE[prov] || []),
                ...(crawledCitiesByProvince[prov] || []),
            ])).sort();
        }
        for (const prov of Object.keys(crawledCitiesByProvince)) {
            if (!citiesByProvince[prov]) citiesByProvince[prov] = crawledCitiesByProvince[prov];
        }

        const provinces = Array.from(new Set([...ALL_PROVINCES, ...(data.provinces || [])])).sort();
        fillSelect(provSel, provinces, 'Any province', initialProvince);
        if (provSel.value) {
            fillSelect(citySel, citiesByProvince[provSel.value] || [], 'Any city', initialCity);
            citySel.disabled = false;
        } else {
            fillSelect(citySel, [], 'Select a province first', '');
            citySel.disabled = true;
        }
    } catch (e) { /* no facets yet */ }
}

function fillSelect(sel, values, anyLabel, preselect) {
    const current = sel.value;
    sel.innerHTML = `<option value="">${anyLabel}</option>` +
        values.map(v => `<option value="${escapeHtml(v)}">${escapeHtml(v)}</option>`).join('');
    if (preselect && values.includes(preselect)) sel.value = preselect;
    else if (current && values.includes(current)) sel.value = current;
}

if (HAS_FILTERS) {
    provSel.addEventListener('change', () => {
        if (provSel.value) {
            fillSelect(citySel, citiesByProvince[provSel.value] || [], 'Any city', '');
            citySel.disabled = false;
        } else {
            fillSelect(citySel, [], 'Select a province first', '');
            citySel.disabled = true;
        }
        runCurrent();
    });
    citySel.addEventListener('change', () => runCurrent());
    clearBtn.addEventListener('click', () => {
        provSel.value = '';
        fillSelect(citySel, [], 'Select a province first', '');
        citySel.disabled = true;
        runCurrent();
    });
}

function filterQuery() {
    const parts = [];
    if (provSel && provSel.value) parts.push('province=' + encodeURIComponent(provSel.value));
    if (citySel && citySel.value) parts.push('city='     + encodeURIComponent(citySel.value));
    return parts.length ? '&' + parts.join('&') : '';
}

// ----- Autocomplete ---------------------------------------------------------
let acTimer = null;
q.addEventListener('input', () => {
    clearTimeout(acTimer);
    acTimer = setTimeout(async () => {
        const term = q.value.trim();
        if (term.length < 2) { acList.style.display = 'none'; return; }
        try {
            const data = await api.get('/api/autocomplete?q=' + encodeURIComponent(term));
            renderAutocomplete(data.suggestions || []);
        } catch (e) { acList.style.display = 'none'; }
    }, 120);
});

function renderAutocomplete(words) {
    if (!words.length) { acList.style.display = 'none'; return; }
    acList.innerHTML = words.map(w => `<div>${escapeHtml(w)}</div>`).join('');
    acList.style.display = 'block';
    [...acList.children].forEach(el => el.onclick = () => {
        q.value = el.textContent;
        acList.style.display = 'none';
        document.getElementById('searchForm').dispatchEvent(new Event('submit'));
    });
}
document.addEventListener('click', e => {
    if (!acList.contains(e.target) && e.target !== q) acList.style.display = 'none';
});

// ----- Search ---------------------------------------------------------------
async function runSearch(query) {
    dym.innerHTML = '';
    if (countEl) countEl.textContent = '';
    results.innerHTML = skeletonList(3);
    try {
        const data = await api.get(`/api/search?type=${TYPE}&q=${encodeURIComponent(query)}${filterQuery()}`);
        if (data.didYouMean) {
            dym.innerHTML = `<div class="did-you-mean">
                Did you mean <a href="#" id="dymLink">${escapeHtml(data.didYouMean)}</a>?
            </div>`;
            document.getElementById('dymLink').onclick = (ev) => {
                ev.preventDefault();
                q.value = data.didYouMean;
                document.getElementById('searchForm').dispatchEvent(new Event('submit'));
            };
        }
        renderCount(data.results.length, query);
        if (!data.results.length) {
            results.innerHTML = `<div class="card muted">
                <h3>No matches.</h3>
                <p class="muted">Try a broader term or clear filters.</p>
            </div>`;
            return;
        }
        results.innerHTML = data.results.map(renderResult).join('');
    } catch (e) {
        results.innerHTML = `<div class="card muted">Error: ${escapeHtml(e.message)}</div>`;
    }
}

async function runTopPicks() {
    dym.innerHTML = '';
    if (countEl) countEl.textContent = '';
    results.innerHTML = skeletonList(3);
    try {
        const data = await api.get(`/api/pagerank?type=${TYPE}${filterQuery()}`);
        renderCount((data.top || []).length, null);
        if (!data.top.length) {
            const filtersActive = (provSel && provSel.value) || (citySel && citySel.value);
            results.innerHTML = filtersActive
                ? `<div class="card muted">No ${TYPE} match the current filters. <a href="#" id="clearLink">Clear filters</a></div>`
                : await emptyIndexCard();
            const c = document.getElementById('clearLink');
            if (c) c.onclick = (e) => { e.preventDefault(); clearBtn && clearBtn.click(); };
            return;
        }
        results.innerHTML = data.top.map(renderResult).join('');
    } catch (e) {
        results.innerHTML = `<div class="card muted">Error: ${escapeHtml(e.message)}</div>`;
    }
}

/**
 * Empty-state when no filter is active and the index is genuinely empty.
 * Hits /api/debug to distinguish "category not yet crawled" (live scrape
 * still running) from "everything's been crawled but truly returned 0".
 */
async function emptyIndexCard() {
    try {
        const stats = await fetch('/api/debug').then(r => r.json());
        const count = (stats.documentsByType || {})[TYPE] || 0;
        if (count === 0) {
            return `<div class="card muted">
                <h3>Still indexing ${TYPE}…</h3>
                <p>The crawler hasn't finished its first pass yet. Refresh the page in a few seconds — curated entries appear within a second of startup, scraped entries take ~30&ndash;60s per category.</p>
            </div>`;
        }
        return `<div class="card muted">No ${TYPE} available yet.</div>`;
    } catch (e) {
        return `<div class="card muted">No ${TYPE} available yet.</div>`;
    }
}

function renderCount(n, query) {
    if (!countEl) return;
    const filterBits = [];
    if (provSel && provSel.value) filterBits.push('province=' + provSel.value);
    if (citySel && citySel.value) filterBits.push('city='     + citySel.value);
    const suffix = filterBits.length ? ` · filtered by <b>${filterBits.join(' · ')}</b>` : '';
    const qBit   = query ? ` for "<b>${escapeHtml(query)}</b>"` : '';
    countEl.innerHTML = `Showing <b>${n}</b> ${TYPE}${qBit}${suffix}`;
}

function runCurrent() {
    const query = q.value.trim();
    if (query) runSearch(query); else runTopPicks();
}

document.getElementById('searchForm').addEventListener('submit', (e) => {
    e.preventDefault();
    const query = q.value.trim();
    if (!query) return runTopPicks();
    runSearch(query);
});
document.getElementById('topBtn').addEventListener('click', runTopPicks);

// Boot: load facets first, then auto-display.
(async () => {
    await loadFacets();
    runCurrent();
})();
