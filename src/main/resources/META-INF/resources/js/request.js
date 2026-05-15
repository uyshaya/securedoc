/* ─ NAVIGATION ─ */
function goTo(id) {
  document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
  document.getElementById(id).classList.add('active');
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

/* Called from the JSF p:commandButton oncomplete on the landing scene.
   Pulls the picked cert + org from the JSF-rendered DOM and writes them
   into the static display elements on the later Details scene. */
function populateFromLanding() {
  const sel = document.getElementById('landingForm:certType');
  if (sel) {
    const opt = sel.selectedIndex >= 0 ? sel.options[sel.selectedIndex] : null;
    if (opt && opt.value) {
      document.getElementById('formCertName').textContent = opt.text;
    }
  }
  const orgInput = document.getElementById('landingForm:orgPicker_input');
  if (orgInput && orgInput.value) {
    document.getElementById('formCertCat').textContent = orgInput.value;
  }
}

/* ─ EMAIL STEP ─
   Client-side regex here is purely UX: it
   gates the Send button until the email looks valid. Server-side
   validation is authoritative. */
function onEmailInput() {
  const emailField = document.getElementById('emailForm:emailInput');
  if (!emailField) return;
  const valid = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailField.value.trim());
  const sendButton = document.getElementById('emailForm:btnSendOtp');
  if (sendButton) sendButton.disabled = !valid;
}

/* Called from the JSF p:commandButton oncomplete on the email scene.
   Pushes the just-sent email into the OTP scene's display and into
   detailsForm:fEmail. The latter is bean-bound and readonly, but the
   send button only AJAX-updates emailForm, so the field would otherwise
   stay blank until detailsForm itself is re-rendered.
   (JSF prefixes input ids with the parent form's NamingContainer, so
   the rendered DOM id is "detailsForm:fEmail".) */
function enterOtpScene() {
  const emailField = document.getElementById('emailForm:emailInput');
  const email = emailField ? emailField.value.trim() : '';
  const displaySpan = document.getElementById('otpEmailDisplay');
  if (displaySpan) displaySpan.textContent = email;
  const detailsEmailField = document.getElementById('detailsForm:fEmail');
  if (detailsEmailField) detailsEmailField.value = email;
  resetOtp();
  goTo('p-otp');
  startResendTimer(30);
}

/* ─ OTP STEP ─
   Six cell inputs are plain HTML for UX (focus shuffling, paste handling).
   On the 6th digit they sync the joined code into a hidden JSF input
   (otpForm:otpInput) and trigger a hidden JSF submit button. The
   server-side verify decision arrives via onVerifyOtpComplete(). */
let otpDigits = ['', '', '', '', '', ''];

function resetOtp() {
  otpDigits = ['', '', '', '', '', ''];
  for (let index = 0; index < 6; index++) {
    const cell = document.getElementById('otp' + index);
    if (cell) {
      cell.value = '';
      cell.className = 'otp-cell';
    }
  }
}

document.addEventListener('DOMContentLoaded', () => {
  for (let index = 0; index < 6; index++) {
    const cell = document.getElementById('otp' + index);
    if (!cell) continue;

    cell.addEventListener('input', event => {
      const digitsOnly = event.target.value.replace(/\D/g, '');
      cell.value = digitsOnly ? digitsOnly[digitsOnly.length - 1] : '';
      otpDigits[index] = cell.value;
      if (cell.value) {
        cell.classList.add('filled');
        if (index < 5) document.getElementById('otp' + (index + 1)).focus();
        else submitOtp();
      } else {
        cell.classList.remove('filled');
      }
    });

    cell.addEventListener('keydown', event => {
      if (event.key === 'Backspace' && !cell.value && index > 0) {
        document.getElementById('otp' + (index - 1)).focus();
      }
      if (event.key === 'ArrowLeft' && index > 0) document.getElementById('otp' + (index - 1)).focus();
      if (event.key === 'ArrowRight' && index < 5) document.getElementById('otp' + (index + 1)).focus();
    });

    cell.addEventListener('paste', event => {
      event.preventDefault();
      const pasted = (event.clipboardData || window.clipboardData).getData('text').replace(/\D/g, '').slice(0, 6);
      for (let pasteIndex = 0; pasteIndex < pasted.length; pasteIndex++) {
        const pasteCell = document.getElementById('otp' + pasteIndex);
        if (pasteCell) {
          pasteCell.value = pasted[pasteIndex];
          otpDigits[pasteIndex] = pasted[pasteIndex];
          pasteCell.classList.add('filled');
        }
      }
      if (pasted.length === 6) submitOtp();
      else if (pasted.length > 0) document.getElementById('otp' + Math.min(pasted.length, 5)).focus();
    });
  }
});

/* Sync the joined 6-digit code into the hidden JSF input and click
   the hidden p:commandButton so the server can verify. */
function submitOtp() {
  const joinedCode = otpDigits.join('');
  if (joinedCode.length !== 6) return;
  const hidden = document.getElementById('otpForm:otpInput');
  const verifyButton = document.getElementById('otpForm:btnVerify');
  if (!hidden || !verifyButton) return;
  hidden.value = joinedCode;
  verifyButton.click();
}

/* Called from the JSF p:commandButton oncomplete after server verify.
   On success: animate cells to success and advance scene.
   On failure: animate cells to error, clear, refocus first cell. */
function onVerifyOtpComplete(args) {
  if (args && !args.validationFailed) {
    for (let index = 0; index < 6; index++) {
      const cell = document.getElementById('otp' + index);
      if (cell) cell.classList.replace('filled', 'success');
    }
    setTimeout(() => goTo('p-form'), 500);
    return;
  }
  for (let index = 0; index < 6; index++) {
    const cell = document.getElementById('otp' + index);
    if (cell) {
      cell.classList.remove('filled');
      cell.classList.add('error');
    }
  }
  setTimeout(() => {
    for (let index = 0; index < 6; index++) {
      const cell = document.getElementById('otp' + index);
      if (cell) {
        cell.classList.remove('error');
        cell.value = '';
      }
      otpDigits[index] = '';
    }
    const firstCell = document.getElementById('otp0');
    if (firstCell) firstCell.focus();
  }, 900);
}

let resendInterval;
function startResendTimer(seconds) {
  const resendButton = document.getElementById('btnResend');
  const timerSpan = document.getElementById('resendTimer');
  if (!resendButton || !timerSpan) return;
  resendButton.disabled = true;
  clearInterval(resendInterval);
  let remaining = seconds;
  timerSpan.textContent = `(${remaining}s)`;
  resendInterval = setInterval(() => {
    remaining--;
    if (remaining <= 0) {
      clearInterval(resendInterval);
      resendButton.disabled = false;
      timerSpan.textContent = '';
    } else {
      timerSpan.textContent = `(${remaining}s)`;
    }
  }, 1000);
}

function resendOtp() {
  resetOtp();
  const firstCell = document.getElementById('otp0');
  if (firstCell) firstCell.focus();
  startResendTimer(30);
}

/* ─ FORM STEP ─ */
function onPurposeChange() {
  const purposeField = document.getElementById('detailsForm:fPurpose');
  if (!purposeField) return;
  const wrap = document.getElementById('otherPurposeWrap');
  if (wrap) wrap.style.display = purposeField.value === 'other' ? 'flex' : 'none';
}

function onFileSelected(event) {
  const file = event.target.files[0];
  if (!file) return;
  const fileNameLabel = document.getElementById('uploadFname');
  if (fileNameLabel) {
    fileNameLabel.textContent = '📎 ' + file.name;
    fileNameLabel.style.display = 'block';
  }
  const zone = document.getElementById('uploadZone');
  if (zone) {
    zone.style.borderColor = 'var(--navy-mid)';
    zone.style.background = 'var(--blue-tint)';
  }
}

/* Returns the trimmed value of a JSF-prefixed input id (or empty string).
   PrimeFaces wraps datePicker/selectOneMenu so the underlying form input
   actually has id "<base>_input"; check that as a fallback. */
function detailsValue(suffix) {
  const fullId = 'detailsForm:' + suffix;
  const direct = document.getElementById(fullId);
  if (direct && 'value' in direct && direct.value !== undefined) {
    return (direct.value || '').trim();
  }
  for (const innerSuffix of ['_input', '_focus']) {
    const inner = document.getElementById(fullId + innerSuffix);
    if (inner && 'value' in inner) return (inner.value || '').trim();
  }
  return '';
}

/* Reads the visible label of a PrimeFaces selectOneMenu (the chosen
   option's display text). Falls back to the raw value for plain inputs. */
function detailsLabel(suffix) {
  const labelNode = document.getElementById('detailsForm:' + suffix + '_label');
  if (labelNode && labelNode.textContent) {
    const text = labelNode.textContent.trim();
    if (text && !text.startsWith('Select')) return text;
  }
  return detailsValue(suffix);
}

/* ─ REVIEW STEP ─
   Called from the details-form p:commandButton oncomplete on success.
   Builds the review list from the just-submitted DOM (values were sent
   to the bean by the JSF submit but we read DOM here to keep this
   purely client-side). */
function enterReviewScene() {
  const certSelect = document.getElementById('landingForm:certType');
  const certOption = certSelect && certSelect.selectedIndex >= 0
      ? certSelect.options[certSelect.selectedIndex] : null;

  const fullName = [
    detailsValue('fFirst'),
    detailsValue('fMiddle'),
    detailsValue('fLast')
  ].filter(Boolean).join(' ') || '—';

  const sexValue = detailsValue('fSex');
  const sexLabel = sexValue === 'M' ? 'Male' : sexValue === 'F' ? 'Female' : '—';

  const purposeValue = detailsValue('fPurpose');
  const purposeDisplay = purposeValue === 'other'
      ? (detailsValue('fOtherPurpose') || '—')
      : (detailsLabel('fPurpose') || '—');

  const rows = [
    { key: 'Certificate Type', value: certOption ? certOption.text : '—' },
    { key: 'Full Name', value: fullName },
    { key: 'Date of Birth', value: formatDate(detailsValue('fDob')) },
    { key: 'Sex', value: sexLabel },
    { key: 'Contact Number', value: detailsValue('fPhone') || '—' },
    { key: 'Email Address', value: detailsValue('fEmail') || '—' },
    { key: 'Purpose', value: purposeDisplay },
    { key: 'Valid ID Type', value: detailsLabel('fIdType') || '—' },
    { key: 'ID Uploaded', value: document.getElementById('uploadFname')?.textContent ? 'Yes ✓' : 'No' },
  ];

  const listNode = document.getElementById('reviewList');
  if (listNode) {
    listNode.innerHTML = rows.map(row => `
    <div class="review-item">
      <span class="rv-key">${row.key}</span>
      <span class="rv-val">${row.value}</span>
    </div>`).join('');
  }

  goTo('p-review');
}

function formatDate(value) {
  if (!value) return '—';
  return new Date(value + 'T00:00:00').toLocaleDateString('en-PH', {
    year: 'numeric', month: 'long', day: 'numeric'
  });
}

/* ─ SUBMIT ─
   The Submit Request button on p-review is a JSF p:commandButton wired
   to RequestBean.submitRequest, which generates the UUID reference and
   exposes it on submittedReference. The button's update=":confirmForm"
   refreshes the confirm scene's <h:outputText> bindings for both the
   reference and the email, then oncomplete calls goTo('p-confirm'). */

function copyRef() {
  const ref = document.getElementById('refNum').textContent;
  navigator.clipboard?.writeText(ref).then(() => {
    const btn = document.getElementById('btnCopy');
    btn.innerHTML = `<i class="pi pi-check" style="font-size:13px;margin-right:5px"></i> Copied!`;
    btn.style.color = 'var(--green)';
    setTimeout(() => {
      btn.innerHTML = `<i class="pi pi-copy" style="font-size:13px;margin-right:5px"></i> Copy`;
      btn.style.color = '';
    }, 2000);
  });
}

function startOver() {
  /* Reset just the landing-scene JSF dropdown DOM value; the session-scoped
     RequestBean keeps its own state until the next AJAX postback overwrites it. */
  const sel = document.getElementById('landingForm:certType');
  if (sel) sel.selectedIndex = 0;
  resetOtp();
  goTo('p-landing');
}

/* ─ TRACK STATUS ─
   Lookup is a stub until the requests-table backend is wired. The input
   + Check button stay live; doTrack always shows the "not found" notice
   since there is nothing to look up yet. renderTrackResult / status
   config moved out with the mock data. */
function onTrackInput() {
  const v = document.getElementById('trackInput').value.trim();
  document.getElementById('btnTrack').disabled = v.length < 3;
  document.getElementById('trackErr').style.display = 'none';
}

function doTrack() {
  document.getElementById('trackErr').style.display = 'flex';
}

function goToTrackFromConfirm() {
  const ref = document.getElementById('refNum').textContent.trim();
  goTo('p-track');
  if (ref) {
    const inp = document.getElementById('trackInput');
    inp.value = ref;
    onTrackInput();
  }
}
