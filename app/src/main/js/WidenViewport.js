(function () {
    'use strict';

    var width = 1024;
    var metaViewport = document.querySelector('meta[name="viewport"]')
    metaViewport.setAttribute('content', 'width=' + width + ', initial-scale=' + (window.screen.width / width));

}());