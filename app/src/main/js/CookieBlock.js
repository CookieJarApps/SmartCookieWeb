// Copyright 2020 CookieJarApps
var styles = '[class*="ConsentBanner"], #cookieChoiceInfo, #cookie-notice, .type-bottom, .cc_cookieAlert, .message-container, .rgpd-notice, #cookiemessage-root, #privacy_policy_wrapper, .cc_overlay_lock, .banner-gdpr, .eu-cookies-show, #cookies-consent, #privconsentContainer, #cookieConsentGTM, #js-cookie-banner, #ma-cookie-law-info, #ma-cookies-law-info, #js--tracking--note, #ideocookie-widget, #headerCookieInfo, .wx-cookie-ppp, .ip-cookie-banner, .cookie-consent--GDPR, #gd-cookiebar, .bottom-bar-cookies, #stickyCookieBar, .app_gdpr--2k2uB.css, -tes4ja-ConsentBanner, .qc-cmp2-container, .qc-cmp-ui-container, .cc_banner-wrapper, .cc-floating, #cookiePrompt, .kLCTYz, #gdpr-banner, #popup-announcements, .cookie-ui__cookieUI___3fxp1, #cookieWarning, #CybotCookiebotDialog, #catapult-cookie-bar, .FAVrq, #gpdr, .gpdr, .we-love-cookies, #privacy_notice, .cookiebar-bar, #stickyCookieBar, #onetrust-consent-sdk, .qc-cmp-cleanslate, .js-cookie-msg, #consent, .qc-cmp2-container, .lmZkxY, .cookiebanner, .bbccookies-banner, .butterBar-message, .gl-modal__main-content, .md-cookiesoptinout, .lOPC8 , .cp-overlay, .cp-dialog, .cc-light, .xFNJP, .yAVMkd, .vk_c, .evidon-consent-button, .cookie-warn, .cc-banner, .cc-bottom, .qc-cmp-ui-content, .hnf-banner, .m-privacy-consent, .c-cookie-disclaimer, .important-banner--cookies, .cookie-policy, .cookie-banner-optout, .cookie-banner__wrapper { visibility: hidden !important; height: 0; } .qc-cmp-ui-showing{ overflow:scroll; }'

var styleSheet = document.createElement("style")
styleSheet.type = "text/css"
styleSheet.innerText = styles
document.head.appendChild(styleSheet)

var elems = document.querySelectorAll(".qc-cmp-ui-showing");

[].forEach.call(elems, function(el) {
    el.classList.remove("qc-cmp-ui-showing");
});
