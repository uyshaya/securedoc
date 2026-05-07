/* ─ CERT HINTS ─ */
const hints = {
  birth: 'Certified true copy of birth record from PSA.',
  marriage: 'Certified true copy of marriage record from PSA.',
  death: 'Certified true copy of death record from PSA.',
  cenomar: 'Proof that a person has never contracted a marriage.',
  'brgy-clearance': 'General-purpose clearance issued by your barangay.',
  'brgy-residency': 'Proof that you reside within the barangay.',
  'brgy-indigency': 'Proof of low-income status for assistance programs.',
  'good-conduct': 'Character clearance for employment or travel.',
  'late-reg': 'Registration of a civil event recorded past the deadline.',
};

function onCertChange() {
  const v = document.getElementById('certType').value;
  document.getElementById('btnProceed').disabled = !v;
  document.getElementById('certHint').textContent = v ? hints[v] : 'Choose the document you need to request.';
}

/* ─ NAVIGATION ─ */
function goTo(id) {
  document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
  document.getElementById(id).classList.add('active');
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function goToEmail() {
  const sel = document.getElementById('certType');
  const opt = sel.options[sel.selectedIndex];
  document.getElementById('formCertName').textContent = opt.text;
  document.getElementById('formCertCat').textContent = opt.closest('optgroup')?.label || '—';
  goTo('p-email');
}

/* ─ EMAIL STEP ─ */
function onEmailInput() {
  const v = document.getElementById('emailInput').value.trim();
  const valid = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v);
  document.getElementById('btnSendOtp').disabled = !valid;
  document.getElementById('emailErr').classList.remove('show');
}

function sendOtp() {
  const email = document.getElementById('emailInput').value.trim();
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    document.getElementById('emailErr').classList.add('show');
    return;
  }
  document.getElementById('otpEmailDisplay').textContent = email;
  document.getElementById('fEmail').value = email;
  resetOtp();
  goTo('p-otp');
  startResendTimer(30);
}

/* ─ OTP STEP ─ */
const DEMO_OTP = '123456';
let otpVal = ['', '', '', '', '', ''];

function resetOtp() {
  otpVal = ['', '', '', '', '', ''];
  for (let i = 0; i < 6; i++) {
    const c = document.getElementById('otp' + i);
    c.value = '';
    c.className = 'otp-cell';
  }
  document.getElementById('otpErr').classList.remove('show');
}

document.addEventListener('DOMContentLoaded', () => {
  for (let i = 0; i < 6; i++) {
    const c = document.getElementById('otp' + i);
    if (!c) continue;

    c.addEventListener('input', e => {
      const val = e.target.value.replace(/\D/g, '');
      c.value = val ? val[val.length - 1] : '';
      otpVal[i] = c.value;
      if (c.value) {
        c.classList.add('filled');
        if (i < 5) document.getElementById('otp' + (i + 1)).focus();
        else checkOtp();
      } else {
        c.classList.remove('filled');
      }
    });

    c.addEventListener('keydown', e => {
      if (e.key === 'Backspace' && !c.value && i > 0) {
        document.getElementById('otp' + (i - 1)).focus();
      }
      if (e.key === 'ArrowLeft' && i > 0) document.getElementById('otp' + (i - 1)).focus();
      if (e.key === 'ArrowRight' && i < 5) document.getElementById('otp' + (i + 1)).focus();
    });

    c.addEventListener('paste', e => {
      e.preventDefault();
      const pasted = (e.clipboardData || window.clipboardData).getData('text').replace(/\D/g, '').slice(0, 6);
      for (let j = 0; j < pasted.length; j++) {
        const cell = document.getElementById('otp' + j);
        if (cell) { cell.value = pasted[j]; otpVal[j] = pasted[j]; cell.classList.add('filled'); }
      }
      if (pasted.length === 6) checkOtp();
      else if (pasted.length > 0) document.getElementById('otp' + Math.min(pasted.length, 5)).focus();
    });
  }
});

function checkOtp() {
  const entered = otpVal.join('');
  if (entered === DEMO_OTP) {
    for (let i = 0; i < 6; i++) document.getElementById('otp' + i).classList.replace('filled', 'success');
    document.getElementById('otpErr').classList.remove('show');
    setTimeout(() => goTo('p-form'), 500);
  } else if (entered.length === 6) {
    for (let i = 0; i < 6; i++) {
      const c = document.getElementById('otp' + i);
      c.classList.remove('filled');
      c.classList.add('error');
    }
    document.getElementById('otpErr').classList.add('show');
    setTimeout(() => {
      for (let i = 0; i < 6; i++) {
        const c = document.getElementById('otp' + i);
        c.classList.remove('error');
        c.value = '';
        otpVal[i] = '';
      }
      document.getElementById('otp0').focus();
    }, 900);
  }
}

let resendInterval;
function startResendTimer(sec) {
  const btn = document.getElementById('btnResend');
  const timer = document.getElementById('resendTimer');
  btn.disabled = true;
  clearInterval(resendInterval);
  let s = sec;
  timer.textContent = `(${s}s)`;
  resendInterval = setInterval(() => {
    s--;
    if (s <= 0) { clearInterval(resendInterval); btn.disabled = false; timer.textContent = ''; }
    else timer.textContent = `(${s}s)`;
  }, 1000);
}

function resendOtp() {
  resetOtp();
  document.getElementById('otp0').focus();
  startResendTimer(30);
}

/* ─ FORM STEP ─ */
function onPurposeChange() {
  document.getElementById('otherPurposeWrap').style.display =
    document.getElementById('fPurpose').value === 'other' ? 'flex' : 'none';
}

function onFileSelected(e) {
  const f = e.target.files[0]; if (!f) return;
  const nm = document.getElementById('uploadFname');
  nm.textContent = '📎 ' + f.name; nm.style.display = 'block';
  const z = document.getElementById('uploadZone');
  z.style.borderColor = 'var(--navy-mid)'; z.style.background = 'var(--blue-tint)';
}

/* ─ REVIEW STEP ─ */
function goToReview() {
  const sel = document.getElementById('certType');
  const opt = sel.options[sel.selectedIndex];
  const purposeSel = document.getElementById('fPurpose');
  const purposeOpt = purposeSel.options[purposeSel.selectedIndex];
  const idSel = document.getElementById('fIdType');
  const idOpt = idSel.options[idSel.selectedIndex];

  const fullName = [
    document.getElementById('fFirst').value.trim(),
    document.getElementById('fMiddle').value.trim(),
    document.getElementById('fLast').value.trim()
  ].filter(Boolean).join(' ') || '—';

  const rows = [
    { k: 'Certificate Type', v: opt.text },
    { k: 'Full Name', v: fullName },
    { k: 'Date of Birth', v: fmtDate(document.getElementById('fDob').value) },
    { k: 'Sex', v: document.getElementById('fSex').value === 'M' ? 'Male' : document.getElementById('fSex').value === 'F' ? 'Female' : '—' },
    { k: 'Contact Number', v: document.getElementById('fPhone').value.trim() || '—' },
    { k: 'Email Address', v: document.getElementById('fEmail').value || '—' },
    { k: 'Purpose', v: purposeOpt?.value === 'other' ? (document.getElementById('fOtherPurpose').value || '—') : (purposeOpt?.text || '—') },
    { k: 'Valid ID Type', v: idOpt?.value ? idOpt.text : '—' },
    { k: 'ID Uploaded', v: document.getElementById('uploadFname').textContent ? 'Yes ✓' : 'No' },
  ];

  document.getElementById('reviewList').innerHTML = rows.map(r => `
    <div class="review-item">
      <span class="rv-key">${r.k}</span>
      <span class="rv-val">${r.v}</span>
    </div>`).join('');

  goTo('p-review');
}

function fmtDate(v) {
  if (!v) return '—';
  return new Date(v + 'T00:00:00').toLocaleDateString('en-PH', { year: 'numeric', month: 'long', day: 'numeric' });
}

/* ─ SUBMIT ─ */
function submitRequest() {
  const ref = 'SC-' + new Date().getFullYear() + '-' + String(Math.floor(10000 + Math.random() * 90000));
  document.getElementById('refNum').textContent = ref;
  document.getElementById('confirmEmail').textContent = document.getElementById('fEmail').value;
  goTo('p-confirm');
}

function copyRef() {
  const ref = document.getElementById('refNum').textContent;
  navigator.clipboard?.writeText(ref).then(() => {
    const btn = document.getElementById('btnCopy');
    btn.innerHTML = `<svg width="14" height="14" viewBox="0 0 16 16" fill="none"><path d="M3 8l4 4 6-6" stroke="var(--green)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg> Copied!`;
    btn.style.color = 'var(--green)';
    setTimeout(() => {
      btn.innerHTML = `<svg width="14" height="14" viewBox="0 0 16 16" fill="none"><rect x="6" y="6" width="8" height="8" rx="1.5" stroke="currentColor" stroke-width="1.3"/><path d="M4 10H3a1 1 0 01-1-1V3a1 1 0 011-1h6a1 1 0 011 1v1" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/></svg> Copy`;
      btn.style.color = '';
    }, 2000);
  });
}

function startOver() {
  document.getElementById('certType').value = '';
  document.getElementById('certHint').textContent = 'Choose the document you need to request.';
  document.getElementById('btnProceed').disabled = true;
  resetOtp();
  goTo('p-landing');
}

/* ─ TRACK STATUS ─ */
const DEMO_STATUSES = {
  'SC-2025-00001': { status: 'SUBMITTED',    doc: 'Barangay Clearance',       submitted: 'March 10, 2025', updated: 'March 10, 2025 · 9:42 AM' },
  'SC-2025-00202': { status: 'UNDER REVIEW', doc: 'Certificate of Residency', submitted: 'March 14, 2025', updated: 'March 15, 2025 · 11:05 AM' },
  'SC-2025-00303': { status: 'PROCESSING',   doc: 'Certificate of Indigency', submitted: 'March 12, 2025', updated: 'March 16, 2025 · 2:30 PM' },
  'SC-2025-00404': { status: 'COMPLETED',    doc: 'Barangay Clearance',       submitted: 'March 8, 2025',  updated: 'March 13, 2025 · 4:15 PM' },
  'SC-2025-00505': { status: 'REJECTED',     doc: 'Certificate of Residency', submitted: 'March 11, 2025', updated: 'March 12, 2025 · 10:00 AM' },
};

const STATUS_CONFIG = {
  'SUBMITTED': {
    cls: 'st-submitted',
    icon: `<svg width="18" height="18" viewBox="0 0 20 20" fill="none"><circle cx="10" cy="10" r="8" stroke="#185FA5" stroke-width="1.6"/><path d="M10 6v4.5l2.5 2" stroke="#185FA5" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>`,
    desc: 'Your request has been received and is in the queue. No action is needed from you at this time.',
    notice: '<strong>What to expect next</strong>A staff member will review your submitted documents within 1–2 business days.',
  },
  'UNDER REVIEW': {
    cls: 'st-review',
    icon: `<svg width="18" height="18" viewBox="0 0 20 20" fill="none"><ellipse cx="10" cy="10" rx="8" ry="5" stroke="#B45309" stroke-width="1.5"/><circle cx="10" cy="10" r="2" fill="#B45309"/></svg>`,
    desc: 'A staff member is currently reviewing your request and submitted documents.',
    notice: '<strong>Under review</strong>Your documents are being verified. You will be notified by email once a decision has been made.',
  },
  'PROCESSING': {
    cls: 'st-processing',
    icon: `<svg width="18" height="18" viewBox="0 0 20 20" fill="none"><path d="M10 3a7 7 0 100 14A7 7 0 0010 3z" stroke="#4361C2" stroke-width="1.5" stroke-dasharray="3 2"/><path d="M10 6v4l2 2" stroke="#4361C2" stroke-width="1.5" stroke-linecap="round"/></svg>`,
    desc: 'Your request has been approved and your document is currently being prepared.',
    notice: '<strong>Document in preparation</strong>Your document is being printed and signed. It should be ready for pickup within the next 1–2 business days.',
  },
  'COMPLETED': {
    cls: 'st-completed',
    icon: `<svg width="18" height="18" viewBox="0 0 20 20" fill="none"><circle cx="10" cy="10" r="8" stroke="#1A7A45" stroke-width="1.5"/><path d="M6.5 10l3 3 4-5" stroke="#1A7A45" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/></svg>`,
    desc: 'Your document is ready for pickup. Please visit the office during business hours.',
    notice: '<strong>Ready for pickup</strong>Bring this reference number and a valid ID to the office. Documents not claimed within 30 days may be voided.',
  },
  'REJECTED': {
    cls: 'st-rejected',
    icon: `<svg width="18" height="18" viewBox="0 0 20 20" fill="none"><circle cx="10" cy="10" r="8" stroke="#C0392B" stroke-width="1.5"/><path d="M7 7l6 6M13 7l-6 6" stroke="#C0392B" stroke-width="1.5" stroke-linecap="round"/></svg>`,
    desc: 'Your request was not approved based on the submitted documents.',
    notice: '<strong>What to do next</strong>You may visit the office for details or submit a new request with complete and valid documents.',
  },
};

function onTrackInput() {
  const v = document.getElementById('trackInput').value.trim();
  document.getElementById('btnTrack').disabled = v.length < 3;
  document.getElementById('trackErr').style.display = 'none';
  document.getElementById('trackResult').style.display = 'none';
}

function doTrack() {
  const raw = document.getElementById('trackInput').value.trim().toUpperCase();

  if (!/^SC-\d{4}-\d{2,5}$/.test(raw)) {
    document.getElementById('trackErr').style.display = 'flex';
    document.getElementById('trackResult').style.display = 'none';
    return;
  }

  const record = DEMO_STATUSES[raw];

  if (!record) {
    document.getElementById('trackErr').style.display = 'flex';
    document.getElementById('trackResult').style.display = 'none';
    return;
  }

  document.getElementById('trackErr').style.display = 'none';
  renderTrackResult(raw, record);
}

function renderTrackResult(ref, record) {
  const cfg = STATUS_CONFIG[record.status];

  const banner = document.getElementById('trackBanner');
  banner.className = 'track-status-banner ' + cfg.cls;
  document.getElementById('trackIcon').innerHTML = cfg.icon;
  document.getElementById('trackStatusLabel').textContent = record.status;
  document.getElementById('trackStatusDesc').textContent = cfg.desc;

  document.getElementById('trackRefDisplay').textContent = ref;
  document.getElementById('trackDocType').textContent = record.doc;
  document.getElementById('trackSubmitted').textContent = record.submitted;
  document.getElementById('trackLastUpdate').textContent = record.updated;

  document.getElementById('trackNoticeText').innerHTML = cfg.notice;

  const noticeEl = document.getElementById('trackNotice');
  if (record.status === 'REJECTED') {
    noticeEl.style.background = 'var(--red-tint)';
    noticeEl.style.borderColor = 'var(--red-bdr)';
    noticeEl.style.borderLeft = '3px solid var(--red)';
    noticeEl.querySelector('svg circle').setAttribute('stroke', '#C0392B');
    noticeEl.querySelector('svg path').setAttribute('stroke', '#C0392B');
    noticeEl.style.color = '#7F1D1D';
  } else {
    noticeEl.style.background = 'var(--blue-tint)';
    noticeEl.style.borderColor = 'var(--blue-bdr)';
    noticeEl.style.borderLeft = '3px solid var(--navy-mid)';
    noticeEl.style.color = '#0C447C';
  }

  document.getElementById('trackResult').style.display = 'block';
}

function goToTrackFromConfirm() {
  const ref = document.getElementById('refNum').textContent;
  goTo('p-track');
  if (ref && ref !== 'SC-2025-00000') {
    const inp = document.getElementById('trackInput');
    inp.value = ref;
    onTrackInput();
    doTrack();
  }
}
