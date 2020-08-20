window.onload = function blockCookies() { 
var cookies = document.querySelectorAll('#gpdr, .gpdr, .we-love-cookies, #privacy_notice, .cookiebar-bar, #stickyCookieBar, #onetrust-consent-sdk, .qc-cmp-cleanslate, .js-cookie-msg, #consent, .kLCTYz, .qc-cmp2-container, .lmZkxY, .cookiebanner, .bbccookies-banner, .butterBar-message, .gl-modal__main-content, .md-cookiesoptinout, .lOPC8 , .cp-overlay, .cp-dialog, .cc-light, .xFNJP, .yAVMkd, .vk_c, .evidon-consent-button, .cookie-warn, .cc-banner, .cc-bottom, .qc-cmp-ui-content, .hnf-banner, .m-privacy-consent, .c-cookie-disclaimer, .important-banner--cookies, .cookie-policy, .cookie-banner-optout, .cookie-banner__wrapper');
    cookies.forEach(function(element) {
	element.parentNode.removeChild(element);
    });
}

