// Simple session helpers using sessionStorage
function saveCurrentUser(user) {
    sessionStorage.setItem('currentUser', JSON.stringify(user));
}

function getCurrentUser() {
    const raw = sessionStorage.getItem('currentUser');
    return raw ? JSON.parse(raw) : null;
}

function clearCurrentUser() {
    sessionStorage.removeItem('currentUser');
}

function requireAuth(redirectTo = 'signin.html') {
    if (!getCurrentUser()) {
        window.location.href = redirectTo;
    }
}
