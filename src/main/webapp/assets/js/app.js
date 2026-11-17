/* =====================================================================
   ShopEase front-end behaviour (vanilla JS, progressive enhancement:
   every feature still works with JavaScript disabled).
   ===================================================================== */
(function () {
    'use strict';

    var ctx = document.body.getAttribute('data-ctx') || '';
    var csrfMeta = document.querySelector('meta[name="csrf-token"]');
    var csrf = csrfMeta ? csrfMeta.getAttribute('content') : '';

    /* ---------- Toasts ---------- */
    var stack = document.querySelector('.toast-stack');

    function toast(message, type) {
        if (!stack) return;
        var el = document.createElement('div');
        el.className = 'toast toast-' + (type || 'success');
        el.textContent = message;
        stack.appendChild(el);
        autoHide(el);
    }

    function autoHide(el) {
        setTimeout(function () {
            el.classList.add('hide');
            setTimeout(function () { el.remove(); }, 350);
        }, 4000);
    }

    document.querySelectorAll('[data-toast]').forEach(autoHide);

    /* ---------- Mobile category menu ---------- */
    var toggle = document.querySelector('[data-nav-toggle]');
    var nav = document.querySelector('[data-nav]');
    if (toggle && nav) {
        toggle.addEventListener('click', function () { nav.classList.toggle('open'); });
    }

    /* ---------- Confirmation prompts ---------- */
    document.querySelectorAll('form[data-confirm]').forEach(function (form) {
        form.addEventListener('submit', function (e) {
            if (!window.confirm(form.getAttribute('data-confirm'))) e.preventDefault();
        });
    });

    /* ---------- Auto-submitting selects (sort) ---------- */
    document.querySelectorAll('select[data-autosubmit]').forEach(function (sel) {
        sel.addEventListener('change', function () { sel.form.submit(); });
    });

    /* ---------- Quantity steppers ---------- */
    document.querySelectorAll('[data-qty]').forEach(function (wrap) {
        var input = wrap.querySelector('input');
        var autoSubmit = wrap.hasAttribute('data-autosubmit-qty');
        var timer;
        function commit() {
            if (!autoSubmit) return;
            clearTimeout(timer);
            timer = setTimeout(function () { input.form.submit(); }, 450);
        }
        wrap.querySelectorAll('[data-step]').forEach(function (btn) {
            btn.addEventListener('click', function () {
                var min = parseInt(input.min || '0', 10);
                var max = parseInt(input.max || '99', 10);
                var v = (parseInt(input.value, 10) || 0) + parseInt(btn.getAttribute('data-step'), 10);
                input.value = Math.max(min, Math.min(max, v));
                commit();
            });
        });
        input.addEventListener('change', commit);
    });

    /* ---------- AJAX add-to-cart via the REST API ---------- */
    function setCartCount(n) {
        document.querySelectorAll('[data-cart-count]').forEach(function (el) {
            el.textContent = n;
            el.classList.add('bump');
            setTimeout(function () { el.classList.remove('bump'); }, 250);
        });
    }

    document.querySelectorAll('form.js-add-to-cart').forEach(function (form) {
        form.addEventListener('submit', function (e) {
            if (!window.fetch) return; // fall back to a normal form post
            e.preventDefault();
            var btn = form.querySelector('button[type=submit]');
            var original = btn.textContent;
            btn.disabled = true;
            btn.textContent = 'Adding…';
            var payload = {
                productId: parseInt(form.querySelector('[name=productId]').value, 10),
                quantity: parseInt((form.querySelector('[name=quantity]') || {value: 1}).value, 10) || 1
            };
            fetch(ctx + '/api/cart/items', {
                method: 'POST',
                credentials: 'same-origin',
                headers: {'Content-Type': 'application/json', 'Accept': 'application/json', 'X-CSRF-Token': csrf},
                body: JSON.stringify(payload)
            }).then(function (res) {
                return res.json().then(function (data) { return {ok: res.ok, data: data}; });
            }).then(function (r) {
                if (r.ok && r.data.success) {
                    setCartCount(r.data.cartCount);
                    toast('Added to cart — ' + r.data.cartCount + ' item(s) in your cart', 'success');
                    btn.textContent = 'Added ✓';
                    setTimeout(function () { btn.textContent = original; btn.disabled = false; }, 1200);
                } else {
                    toast(r.data.message || 'Could not add this item.', 'error');
                    btn.textContent = original;
                    btn.disabled = false;
                }
            }).catch(function () {
                btn.textContent = original;
                btn.disabled = false;
                form.submit(); // network/JSON problem - let the server handle it the classic way
            });
        });
    });

    /* ---------- Payment page ---------- */
    var payForm = document.querySelector('[data-payment-form]');
    if (payForm) {
        var radios = payForm.querySelectorAll('[data-pay-method]');
        var panels = payForm.querySelectorAll('[data-panel]');
        var payBtn = payForm.querySelector('[data-pay-button]');
        var payLabel = payBtn ? payBtn.innerHTML : '';

        function showPanel() {
            var checked = payForm.querySelector('[data-pay-method]:checked');
            var method = checked ? checked.value : null;
            panels.forEach(function (p) {
                var active = p.getAttribute('data-panel') === method;
                p.hidden = !active;
                p.querySelectorAll('input, select').forEach(function (i) { i.disabled = !active; });
            });
            if (payBtn) payBtn.innerHTML = method === 'COD' ? 'Place order (Cash on Delivery)' : payLabel;
        }
        radios.forEach(function (r) { r.addEventListener('change', showPanel); });
        if (!payForm.querySelector('[data-pay-method]:checked') && radios.length) radios[0].checked = true;
        showPanel();

        var cardNumber = payForm.querySelector('[data-card-number]');
        if (cardNumber) {
            cardNumber.addEventListener('input', function () {
                var digits = cardNumber.value.replace(/\D/g, '').slice(0, 19);
                cardNumber.value = digits.replace(/(.{4})/g, '$1 ').trim();
            });
        }
        var expiry = payForm.querySelector('[data-card-expiry]');
        if (expiry) {
            expiry.addEventListener('input', function () {
                var d = expiry.value.replace(/\D/g, '').slice(0, 4);
                expiry.value = d.length > 2 ? d.slice(0, 2) + '/' + d.slice(2) : d;
            });
        }
        payForm.addEventListener('submit', function () {
            if (payBtn) {
                payBtn.disabled = true;
                payBtn.textContent = 'Processing payment…';
            }
        });
    }
})();
