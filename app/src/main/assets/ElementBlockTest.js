/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * Created by CookieJarApps 17/12/2020 */

function translate(value, elem){
    $.ajax({
                    type: "POST",
                    url: "https://cookiejarapps.com/translate/index.php?text=" + encodeURIComponent(value),
                    dataType: "json",
                    success: function (result, status, xhr) {
                        console.log(result["text"]);
                        $(elem).text(result["text"]);
                    },
                    error: function (xhr, status, error) {
                       // alert("Result: " + status + " " + error + " " + xhr.status + " " + xhr.statusText)
                    }
                });
    }
    
    function findGetParameter(parameterName) {
        var result = null,
            tmp = [];
        location.search
            .substr(1)
            .split("&")
            .forEach(function (item) {
              tmp = item.split("=");
              if (tmp[0] === parameterName) result = decodeURIComponent(tmp[1]);
            });
        return result;
    }
    
    $(document).ready(function() {
       
    $('h1, h2, h3, h4, label, p, div, span, button, a, li, ul, td').each(function(){
        console.log(encodeURIComponent($(this).text()))
        if(findGetParameter("lang") == "fr" && $(this).children().length == 0 || findGetParameter("lang") == "fr" && $(this).children('br')[0] != null && $(this).children().length == 1 || findGetParameter("lang") == "fr" && $(this).children('b')[0] != null && $(this).children().length == 1){
            $(this).text(translate($(this).text(), this));
        }
    })
});