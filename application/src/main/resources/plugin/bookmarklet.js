javascript:(function(){
    var container = document.getElementsByClassName('external-messages');

    if(container.length == 0) return alert("Wissensbasis has to be open!");

    container[0].style.margin = '0 20px';

    container[0].innerHTML = '<div>Loading</div>';

    var roomDiv = document.querySelector('[id^="chat-window-"]');

    if(!roomDiv) return alert("Cannot get room id");

    var room = roomDiv.id.substring(12);

    var smartiScript = document.createElement('script');

    smartiScript.onload = function() {
        var widget = new SmartiWidget(container,room,{
            inputCssSelector:'.autogrow-shadow'
        });
    };
    document.body.appendChild(smartiScript);
    smartiScript.src='https://dev.cerbot.redlink.io/9503/dist/bundle.js';
})();