// Firebase Authentication (Google Sign-In) bootstrap.
//
// HOW TO ENABLE (after you create your Firebase project):
//   1. Firebase Console -> Project settings -> General -> Your apps -> Web app
//   2. Copy the SDK setup config (the `firebaseConfig` object) and paste it
//      into the variable below, replacing the placeholder values.
//   3. In the same console, Authentication -> Sign-in method -> Google -> Enable
//   4. Save & reload http://localhost:8080
//
// Until you do that, the "Sign in with Google" button does nothing and the
// app continues to work in DEMO mode (backend skips auth).

const firebaseConfig = {
    apiKey:            "AIzaSyCTmLbpoWPiSqk6ktUR_UoFjANSQUSXpWk",
    authDomain:        "new2canada-ca03c.firebaseapp.com",
    projectId:         "new2canada-ca03c",
    storageBucket:     "new2canada-ca03c.firebasestorage.app",
    messagingSenderId: "783449294250",
    appId:             "1:783449294250:web:68a5cf6e09c08df3c857d3",
    measurementId:     "G-Y1GMRKKC73"
};

let firebaseApp = null;
let firebaseAuth = null;
// Resolves after onAuthStateChanged has fired at least once — i.e. when
// Firebase has finished restoring (or rejecting) any persisted session
// from IndexedDB. Callers that need the bearer token MUST await this,
// otherwise they'll hit the API a few hundred ms before currentUser is
// populated and get a 401 from the backend.
let authReady = null;

async function ensureFirebase() {
    if (firebaseApp) return firebaseAuth;
    if (firebaseConfig.apiKey === "YOUR_API_KEY") return null; // not configured yet

    const appMod  = await import("https://www.gstatic.com/firebasejs/10.12.0/firebase-app.js");
    const authMod = await import("https://www.gstatic.com/firebasejs/10.12.0/firebase-auth.js");

    firebaseApp  = appMod.initializeApp(firebaseConfig);
    firebaseAuth = authMod.getAuth(firebaseApp);

    let resolveReady;
    authReady = new Promise(r => { resolveReady = r; });
    let resolved = false;
    authMod.onAuthStateChanged(firebaseAuth, (user) => {
        renderAuthUI(user, authMod);
        if (!resolved) { resolved = true; resolveReady(); }
    });
    window.__firebaseAuthModule__ = authMod; // for sign-in button
    return firebaseAuth;
}

function renderAuthUI(user, authMod) {
    const box = document.getElementById('authBox');
    if (!box) return;
    if (user) {
        const photo = user.photoURL ? `<img src="${user.photoURL}" alt="">` : '';
        box.innerHTML = `
            <span class="user-chip">${photo}<span>Hi, ${user.displayName || user.email}</span></span>
            <button class="gsi-button" id="signOutBtn">Sign out</button>`;
        document.getElementById('signOutBtn').onclick = () => authMod.signOut(firebaseAuth);
    } else {
        box.innerHTML = `<button class="gsi-button" id="signInBtn">
            <span>G</span> Sign in with Google</button>`;
        document.getElementById('signInBtn').onclick = async () => {
            const provider = new authMod.GoogleAuthProvider();
            try { await authMod.signInWithPopup(firebaseAuth, provider); }
            catch (e) { alert('Sign in failed: ' + e.message); }
        };
    }
}

// Exposed to app.js — returns a fresh ID token or null if anonymous.
// Awaits the first onAuthStateChanged firing so we never report "anonymous"
// just because IndexedDB hasn't finished restoring the session yet.
window.getIdToken = async function () {
    const auth = await ensureFirebase();
    if (!auth) return null;
    if (authReady) await authReady;
    if (!auth.currentUser) return null;
    return await auth.currentUser.getIdToken();
};

document.addEventListener('DOMContentLoaded', async () => {
    const auth = await ensureFirebase();
    // Render the placeholder Sign-in button even when firebase isn't configured.
    if (!auth) {
        const box = document.getElementById('authBox');
        if (box) box.innerHTML =
            `<span class="muted" title="DEMO mode">Sign-in disabled — configure Firebase in js/auth.js</span>`;
    }
});
