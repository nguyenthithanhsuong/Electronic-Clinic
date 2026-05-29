// Utility functions for API calls
const API_BASE = 'http://localhost:3001/api';

async function apiGet(endpoint) {
    try {
        const response = await fetch(`${API_BASE}${endpoint}`);
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        return await response.json();
    } catch (error) {
        console.error(`GET ${endpoint} failed:`, error);
        throw error;
    }
}

async function apiPost(endpoint, data) {
    try {
        const response = await fetch(`${API_BASE}${endpoint}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        return await response.json();
    } catch (error) {
        console.error(`POST ${endpoint} failed:`, error);
        throw error;
    }
}

async function apiPut(endpoint, data) {
    try {
        const response = await fetch(`${API_BASE}${endpoint}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        return await response.json();
    } catch (error) {
        console.error(`PUT ${endpoint} failed:`, error);
        throw error;
    }
}

async function apiDelete(endpoint) {
    try {
        const response = await fetch(`${API_BASE}${endpoint}`, { method: 'DELETE' });
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        return await response.json();
    } catch (error) {
        console.error(`DELETE ${endpoint} failed:`, error);
        throw error;
    }
}

function createTable(headers, data, onEdit, onDelete) {
    let html = '<table border="1" cellpadding="8"><tr>';
    headers.forEach(h => html += `<th>${h}</th>`);
    html += '<th>Actions</th></tr>';
    
    data.forEach((row, idx) => {
        html += '<tr>';
        Object.values(row).forEach(val => html += `<td>${val}</td>`);
        html += `<td><button onclick="editItem(${row.id})">Edit</button> <button onclick="deleteItem(${row.id})">Delete</button></td></tr>`;
    });
    html += '</table>';
    return html;
}

function showMessage(msg, type = 'info') {
    const div = document.getElementById('message');
    if (div) {
        div.textContent = msg;
        div.className = type;
        setTimeout(() => div.textContent = '', 5000);
    }
}
