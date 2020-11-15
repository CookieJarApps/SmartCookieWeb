// Copyright 2020 CookieJarApps
var styles = '#uhfCookieAlert, #js-gdpr-consent-banner, #msccBanner, div[id^=\'sp_message_container\'], #sp_message_iframe_200665, ._5m_x _9fyr, #cookieChoiceInfo, .message-overlay, .message-container, .aID8W, .bErdLd, .gu-overlay, #_5vCeX-ntJ_XzxgPg75egDQ6, .nhsuk-cookie-banner, [class*="ConsentBanner"], #cookieChoiceInfo, #cookie-notice, .type-bottom, .cc_cookieAlert, .message-container, .rgpd-notice, #cookiemessage-root, #privacy_policy_wrapper, .cc_overlay_lock, .banner-gdpr, .eu-cookies-show, #cookies-consent, #privconsentContainer, #cookieConsentGTM, #js-cookie-banner, #ma-cookie-law-info, #ma-cookies-law-info, #js--tracking--note, #ideocookie-widget, #headerCookieInfo, .wx-cookie-ppp, .ip-cookie-banner, .cookie-consent--GDPR, #gd-cookiebar, .bottom-bar-cookies, #stickyCookieBar, .app_gdpr--2k2uB.css, -tes4ja-ConsentBanner, .qc-cmp2-container, .qc-cmp-ui-container, .cc_banner-wrapper, .cc-floating, #cookiePrompt, .kLCTYz, #gdpr-banner, #popup-announcements, .cookie-ui__cookieUI___3fxp1, #cookieWarning, #CybotCookiebotDialog, #catapult-cookie-bar, .FAVrq, #gpdr, .gpdr, .we-love-cookies, #privacy_notice, .cookiebar-bar, #stickyCookieBar, #onetrust-consent-sdk, .qc-cmp-cleanslate, .js-cookie-msg, #consent, .qc-cmp2-container, .lmZkxY, .cookiebanner, .bbccookies-banner, .butterBar-message, .gl-modal__main-content, .md-cookiesoptinout, .lOPC8 , .cp-overlay, .cp-dialog, .cc-light, .xFNJP, .yAVMkd, .vk_c, .evidon-consent-button, .cookie-warn, .cc-banner, .cc-bottom, .qc-cmp-ui-content, .hnf-banner, .m-privacy-consent, .c-cookie-disclaimer, .important-banner--cookies, .cookie-policy, .cookie-banner-optout, .cookie-banner__wrapper { visibility: hidden !important; height: 0 !important; } .qc-cmp-ui-showing{ overflow: auto !important; } .QVCmK{ overflow: scroll !important; position: absolute !important; }'

var styleSheet = document.createElement("style")
styleSheet.type = "text/css"
styleSheet.innerText = styles
document.head.appendChild(styleSheet)

// Disabled more advanced popups
var fbPopup = document.getElementsByClassName("_5m_x _9fyr");
while(fbPopup.length > 0){
    fbPopup[0].parentNode.removeChild(fbPopup[0]);
}

var element = document.getElementsByTagName("html")[0]
  element.classList.remove("sp-message-open");
