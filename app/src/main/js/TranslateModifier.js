/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * Created by CookieJarApps 17/12/2020 */

setTimeout(function() {
	document.getElementById("titleDiv").remove();
	document.getElementById("CookieBannerWrapper").remove();
}, 1500);

window.hipUrl = null;
if(document.querySelector('#ToDDL').value != lang){
	document.querySelector('#ToDDL').value = lang; BVLangPair.UpdateToLang(); //BV.onTranslateButtonClick();
}
