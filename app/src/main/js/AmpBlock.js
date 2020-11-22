// Copyright 2020 CookieJarApps MPL
var html = document.getElementsByTagName('html');
if (document.location.href.includes('https://www.google.com/amp/s/')) {

    originalSite = document.getElementsByTagName('link');
    for (var i = 0; i < originalSite.length; i++) {
        if (originalSite[i].getAttribute('rel') != window.location.href.split('#')[0] && originalSite[i].getAttribute('rel') == 'canonical') {
            window.location.replace(originalSite[i].getAttribute('href'));
        }
    }
}