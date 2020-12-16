// Copyright 2020 CookieJarApps MPL
setTimeout(function() {
	document.getElementById("titleDiv").remove();
	document.getElementById("CookieBannerWrapper").remove();
}, 1500);

window.hipUrl = null;
if(document.querySelector('#ToDDL').value != lang){
	document.querySelector('#ToDDL').value = lang; //BVLangPair.UpdateToLang(); BV.onTranslateButtonClick();
}
