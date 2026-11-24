const SESSION_KEY = 'flight_web_session';

export function getSession() {
    try {
        const raw = localStorage.getItem(SESSION_KEY);
        return raw ? JSON.parse(raw) : null;
    } catch (error) {
        return null;
    }
}

export function setSession(session) {
    localStorage.setItem(SESSION_KEY, JSON.stringify(session));
}

export function clearSession() {
    localStorage.removeItem(SESSION_KEY);
}

export function getToken() {
    return getSession()?.token || null;
}

export function getUser() {
    return getSession()?.user || null;
}

export function isLoggedIn() {
    return Boolean(getSession()?.token);
}

export function homeFor(user) {
    return user?.role === 'ADMIN' ? 'reports.html' : 'flights.html';
}

export function requireAuth() {
    const session = getSession();
    if (!session?.token || !session.user) {
        location.replace('login.html');
        throw new Error('未登录');
    }
    return session.user;
}

export function requireAdmin() {
    const user = requireAuth();
    if (user.role !== 'ADMIN') {
        location.replace('flights.html');
        throw new Error('无权限');
    }
    return user;
}
