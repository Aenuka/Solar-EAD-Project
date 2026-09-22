(() => {
    const dialog = document.getElementById('energy-window-dialog');
    const trigger = document.getElementById('add-energy-window');
    if (!dialog || !trigger) return;

    trigger.addEventListener('click', () => dialog.showModal());
    dialog.querySelectorAll('[data-close-window]').forEach(button => {
        button.addEventListener('click', () => dialog.close());
    });
    dialog.addEventListener('close', () => trigger.focus());

    document.querySelectorAll('[data-edit-availability]').forEach(button => {
        const form = document.getElementById(button.getAttribute('aria-controls'));
        const summary = button.closest('section').querySelector('.availability-summary');
        const finishEditing = () => {
            form.hidden = true;
            summary.hidden = false;
            button.hidden = false;
            button.setAttribute('aria-expanded', 'false');
            button.focus();
        };
        button.addEventListener('click', () => {
            form.hidden = false;
            summary.hidden = true;
            button.hidden = true;
            button.setAttribute('aria-expanded', 'true');
            form.querySelector('input:not([type="hidden"])').focus();
        });
        form.querySelector('[data-cancel-availability]').addEventListener('click', () => {
            form.reset();
            finishEditing();
        });
    });

    const confirmation = document.getElementById('station-confirm-dialog');
    const accept = confirmation.querySelector('[data-accept-confirm]');
    let pendingForm;
    let pendingButton;
    document.querySelectorAll('form[data-confirm-title]').forEach(form => {
        form.addEventListener('submit', event => {
            event.preventDefault();
            pendingForm = form;
            pendingButton = event.submitter;
            confirmation.querySelector('#station-confirm-title').textContent = form.dataset.confirmTitle;
            confirmation.querySelector('#station-confirm-message').textContent = form.dataset.confirmMessage;
            accept.textContent = form.dataset.confirmLabel;
            accept.classList.toggle('danger', form.dataset.danger === 'true');
            confirmation.showModal();
        });
    });
    confirmation.querySelector('[data-cancel-confirm]').addEventListener('click', () => confirmation.close());
    confirmation.addEventListener('close', () => {
        pendingButton?.focus();
        pendingForm = null;
    });
    accept.addEventListener('click', () => {
        if (!pendingForm) return;
        accept.disabled = true;
        HTMLFormElement.prototype.submit.call(pendingForm);
    });
})();
