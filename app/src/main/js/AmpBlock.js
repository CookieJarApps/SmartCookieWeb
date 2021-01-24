// Copyright 2020 CookieJarApps MPL
if (document.querySelector('.amp_r') !== null) {
var amp = document.getElementsByClassName("amp_r");
for(var i = 0; i < amp.length; i++){
  amp.item(i).removeAttribute('data-amp');
  amp.item(i).removeAttribute('data-amp-cur');
  amp.item(i).removeAttribute('data-amp-title');
  amp.item(i).removeAttribute('data-amp-vgi');
  amp.item(i).removeAttribute('ping');
  amp.item(i).removeAttribute('jsaction');
  amp.item(i).removeAttribute('oncontextmenu');
  amp.item(i).classList.remove("amp_r");
}
}

// Still AMP :(
var h = document.getElementsByTagName('html');
if (h[0].getAttribute('amp') != null && window.location.href.includes("amp") || h[0].getAttribute('⚡') != null && window.location.href.includes("amp")){
        eles = document.getElementsByTagName('link');
        for (var i=0; i<eles.length;i++){
            if (eles[i].getAttribute('rel') == 'canonical'){
                window.location.replace(eles[i].getAttribute('href'))
            }
        }
}