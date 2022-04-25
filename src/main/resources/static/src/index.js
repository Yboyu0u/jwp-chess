let source = "";
let target = "";

function clicked(id) {
    console.log(id);
    setVariable(id);
    checkSendToServer();
}

function setVariable(id) {
    if (source !== "") {
        target = id;
    } else {
        source = id;
    }
}

function checkSendToServer() {
    if (source !== "" && target !== "") {
        sendToServer(source, target);
        source = "";
        target = "";
    }
}

function sendToServer() {
    fetch('/move', {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({
            source: source,
            target: target
        })
    }).then((response) =>{
        response.json().then(data => {
            if (data.status ===  400) {
                alert(data.errorMessage);
            }
            if(data.isGameOver === true) {
                alert("게임이 종료되었습니다.")
                document.location.href = '/result'
                return;
            };

            location.reload();
        });
    });
}
