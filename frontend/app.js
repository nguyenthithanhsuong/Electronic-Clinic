const API_BASE = 'http://localhost:8080/api';

const state = {
  users: [],
  details: [],
  logs: [],
};

const form = document.getElementById('userForm');
const roleButtons = document.querySelectorAll('.role-btn');
const dynamicLabel = document.getElementById('dynamicLabel');
const dynamicInput = document.getElementById('specialtyOrCondition');
const resultCard = document.getElementById('resultCard');
const resultRole = document.getElementById('resultRole');
const resultId = document.getElementById('resultId');
const resultName = document.getElementById('resultName');
const resultMeta = document.getElementById('resultMeta');
const secondaryTableLabel = document.getElementById('secondaryTableLabel');
const backendStatus = document.getElementById('backendStatus');
const logList = document.getElementById('logList');
const logCount = document.getElementById('logCount');
const usersTable = document.getElementById('usersTable');
const detailTable = document.getElementById('detailTable');
const steps = document.querySelectorAll('#steps li');

let selectedRole = 'Doctor';

function formatTime(date) {
  return date.toLocaleTimeString('vi-VN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  });
}

function renderLogs() {
  logCount.textContent = `${state.logs.length} dòng`;

  if (state.logs.length === 0) {
    logList.innerHTML = '<div class="log-empty">Chưa có log nào.</div>';
    return;
  }

  logList.innerHTML = state.logs
    .map((entry) => `
      <div class="log-item ${entry.tone}">
        <span class="log-time">${entry.time}</span>
        <span class="log-message">${entry.message}</span>
      </div>
    `)
    .join('');
}

function addLog(message, tone) {
  state.logs.unshift({
    message,
    tone: tone || 'info',
    time: formatTime(new Date()),
  });

  if (state.logs.length > 8) {
    state.logs.length = 8;
  }

  renderLogs();
}

function updateBackendStatus(isConnected) {
  backendStatus.textContent = isConnected ? 'Connected Backend' : 'Disconnected Backend';
  backendStatus.classList.toggle('disconnected', !isConnected);
}

async function fetchSnapshot() {
  const response = await fetch(`${API_BASE}/snapshot`);
  if (!response.ok) {
    throw new Error('Cannot load snapshot from backend.');
  }
  return response.json();
}

async function registerUser(formData) {
  const payload = new URLSearchParams();
  payload.set('userType', selectedRole);
  payload.set('fullName', formData.fullName);
  payload.set('specialtyOrCondition', formData.specialtyOrCondition);

  const response = await fetch(`${API_BASE}/register`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
    },
    body: payload.toString(),
  });

  const result = await response.json();
  if (!response.ok) {
    throw new Error(result.error || 'Register request failed.');
  }

  return result;
}

function updateRoleUI(role, shouldLog) {
  selectedRole = role;

  roleButtons.forEach((button) => {
    button.classList.toggle('active', button.dataset.role === role);
  });

  if (role === 'Doctor') {
    dynamicLabel.firstChild.textContent = ' Chuyên khoa';
    dynamicInput.value = dynamicInput.value;
    secondaryTableLabel.textContent = 'Bảng Bác sĩ';
    if (shouldLog !== false) {
      addLog('Chọn Doctor -> sẽ dùng DoctorCreator.', 'info');
    }
  } else {
    dynamicLabel.firstChild.textContent = ' Tình trạng bệnh';
    dynamicInput.value = dynamicInput.value;
    secondaryTableLabel.textContent = 'Bảng Bệnh nhân';
    if (shouldLog !== false) {
      addLog('Chọn Patient -> sẽ dùng PatientCreator.', 'info');
    }
  }
}

function animateSteps() {
  steps.forEach((step, index) => {
    step.classList.toggle('active', index === 0);
  });

  setTimeout(() => {
    steps[1].classList.add('active');
  }, 120);

  setTimeout(() => {
    steps[2].classList.add('active');
  }, 240);

  setTimeout(() => {
    steps[3].classList.add('active');
  }, 360);
}

function renderTables() {
  usersTable.innerHTML = state.users
    .map((user) => `
      <div class="row">
        <span>${String(user.id).padStart(3, '0')}</span>
        <span>${user.fullName}</span>
        <span><span class="role-pill">${user.role}</span></span>
      </div>
    `)
    .join('');

  detailTable.innerHTML = state.details
    .map((item) => `
      <div class="row">
        <span>${String(item.id).padStart(3, '0')}</span>
        <span>${item.type}</span>
        <span class="detail-pill">${item.detail}</span>
      </div>
    `)
    .join('');
}

function showLatestUser(user, formData) {
  resultRole.textContent = user.role;
  resultId.textContent = `#${String(user.id).padStart(3, '0')}`;
  resultName.textContent = user.fullName;
  resultMeta.textContent = formData.specialtyOrCondition;
  resultCard.classList.remove('flash');
  void resultCard.offsetWidth;
  resultCard.classList.add('flash');
}

roleButtons.forEach((button) => {
  button.addEventListener('click', () => {
    updateRoleUI(button.dataset.role);
  });
});

form.addEventListener('submit', async (event) => {
  event.preventDefault();

  const formData = {
    fullName: document.getElementById('fullName').value.trim(),
    specialtyOrCondition: document.getElementById('specialtyOrCondition').value.trim(),
  };

  try {
    addLog('1. Frontend gửi yêu cầu tạo user lên API /register.', 'info');
    const user = await registerUser(formData);
    addLog(`2. UserService chọn ${user.role}Creator theo userType.`, 'info');
    addLog('3. validateRequest() hoàn tất, dữ liệu hợp lệ.', 'info');
    addLog(`4. createUser() tạo ${user.role}User cụ thể.`, 'info');
    addLog(`5. saveProfile() lưu vào Users và bảng ${user.role}.`, 'info');
    addLog('6. audit() ghi nhận account vừa tạo.', 'info');
    const snapshot = await fetchSnapshot();
    state.users = snapshot.users || [];
    state.details = snapshot.details || [];
    addLog(`7. Backend trả về ID #${String(user.id).padStart(3, '0')} và frontend refresh snapshot.`, 'success');

    animateSteps();
    showLatestUser(user, formData);
    renderTables();
    form.reset();
    updateRoleUI(selectedRole, false);
  } catch (error) {
    alert(error.message);
  }
});

async function initialize() {
  updateRoleUI(selectedRole, false);
  try {
    const snapshot = await fetchSnapshot();
    state.users = snapshot.users || [];
    state.details = snapshot.details || [];
    renderTables();
    updateBackendStatus(true);
    addLog('Frontend đã kết nối backend và sẵn sàng demo Factory Method.', 'success');
  } catch (error) {
    updateBackendStatus(false);
    addLog('Không kết nối được backend Java.', 'error');
  }
}

initialize();
