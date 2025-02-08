/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

(function() {
    'use strict';

    var cookieDialogs = {
        selectors: {
            rules: [
                {hostname: /google/, target: '.HTjtHe'},
                {hostname: /polygon/, target: '#privacy-consent'},
                {hostname: /payback/, target: 'div[data-userhiddencontent-name="dsgvo"]'},
                {hostname: /blogspot/, target: '#cookieChoiceInfo'},
                {hostname: /sivantos/, target: '.cookie-confirmation'},
                {hostname: /ikea/, target: '#cookieMsgBlock'},
                {hostname: /paypal/, target: '#gdprCookieBanner'},
                {hostname: /microsoft/, target: '#uhfCookieAlert'},
                {hostname: /facebook/, target: '._5m_x'},
                {hostname: /twitter/, target: '.r-1m3jxhj'},
                {hostname: /citroen/, target: "#_psaihm_main_div,#_psaihm_overlay"},
                {hostname: /nhs/, target: ".nhsuk-cookie-banner"},

                {hostname: /./, target: '.eupopup-container'},
                {hostname: /./, target: '.g-consentmanager'},
                {hostname: /./, target: '.cookie_hint'},
                {hostname: /./, target: '.cookiebanner'},
                {hostname: /./, target: '#cookiebanner'},
                {hostname: /./, target: '#cookieBar'},
                {hostname: /./, target: '#onetrust-consent-sdk'},
                {hostname: /./, target: '.fc-consent-root'},
                {hostname: /./, target: '.cc_banner-wrapper'},
                {hostname: /./, target: '#js-eu-cookie'},
                {hostname: /./, target: '#AcceptCookiesBanner'},
                {hostname: /./, target: '.js-CookieBanner'},
                {hostname: /./, target: '#ck-cookie-statement'},
                {hostname: /./, target: '.cc-window,.cc-banner,.cc-overlay'},
                {hostname: /./, target: '#cookie-law-info-bar'},
                {hostname: /./, target: '#cookie-banner'},
                {hostname: /./, target: '#cmpbox'},
                {hostname: /./, target: '#cmpbox2'},
                {hostname: /./, target: "#cookie-notice"},
                {hostname: /./, target: "#sp-cc"},
                {hostname: /./, target: "#cookie_law"},
                {hostname: /./, target: "#gdpr"},
                {hostname: /./, target: ".responsive-app__cookies"},
                {hostname: /./, target: ".x-cookies"},
                {hostname: /./, target: ".cookie"},
                {hostname: /./, target: "#js-gdpr-cookie-banner"},
                {hostname: /./, target: "#mscc-banner"},
                {hostname: /./, target: "div[id^=\'sp_message_container\']"},
                {hostname: /./, target: ".ConsentBanner"},
                {hostname: /./, target: ".cc-cookieAlert"},
                {hostname: /./, target: ".cc_overlay_lock"},
                {hostname: /./, target: ".banner-gdpr"},
                {hostname: /./, target: "#cookies-consent"},
                {hostname: /./, target: ".cookie-consent--GDPR"},
                {hostname: /./, target: ".c-cookie-disclaimer"},
                {hostname: /./, target: ".important-banner--cookies"},
                {hostname: /./, target: ".cookie-policy"},
                {hostname: /./, target: ".cookie-banner-optout"},
                {hostname: /./, target: ".cookie-banner--wrapper"},
            ]
        }
    };

    function executeRemove(node, selector) {
        for (var i = 0; i < node.length; i++) {
            node[i].remove();
        }
        if (node.length && selector) { console.log('remove', selector); }
    }

    function executeRule(rule) {
        if (typeof rule.hostname == 'string' && rule.hostname !== location.hostname) { return; }
        if (typeof rule.hostname == 'object' && typeof rule.hostname.match == 'function' && !rule.hostname.match(location.hostname)) { return; }

        executeRemove(document.querySelectorAll(rule.target), rule.target);
    }

    function execute() {
        cookieDialogs.selectors.rules.forEach(executeRule);
    }

    function undoScrollLock() {
        const googleLock = document.querySelectorAll(".EM1Mrb");

        [].forEach.call(googleLock, function(el) {
            el.classList.remove("EM1Mrb");
        });

        const consentManagerLock = document.querySelectorAll(".is-display-consentmanager");

        [].forEach.call(consentManagerLock, function(el) {
            el.classList.remove("is-display-consentmanager");
        });

        const body = document.querySelectorAll("body");

        [].forEach.call(body, function(el) {
            el.style.removeProperty('overflow');
        });
    }

    undoScrollLock();
    execute();
})();