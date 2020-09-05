function prepareFrame() {
	document.documentElement.innerHTML = '';
        var ifrm = document.createElement("iframe");
        ifrm.setAttribute("src", "https://translate.googleusercontent.com/translate_c?depth=1&pto=aue&rurl=translate.google.co.uk&sl=en&sp=nmt4&tl=af&u=" + window.location.href + "&usg=ALkJrhjnkD8vmFZdUzfqbMw2bRm3KfMrNA");
        ifrm.style.width = window.innerWidth  + "px";
        ifrm.style.height = window.innerHeight  + "px";
	ifrm.style.border = "0px";
	ifrm.style.margin = "0px";
        document.body.appendChild(ifrm);
    }

prepareFrame()