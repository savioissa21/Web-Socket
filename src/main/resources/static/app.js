let stompClient = null;
const statusBadge = document.getElementById('statusBadge');
const cardsContainer = document.getElementById('cardsContainer');

// Store active cities to prevent duplicate cards and manage updates
const activeCities = new Map();

function connect() {
    // EndPoint configurado no Spring Boot
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);

    // Desativar logs de debug do STOMP no console (opcional)
    stompClient.debug = null;

    stompClient.connect({}, function (frame) {
        setConnected(true);
        console.log('Conectado: ' + frame);
        
        // Inscreve-se no tópico de clima
        stompClient.subscribe('/topic/clima', function (message) {
            const data = JSON.parse(message.body);
            updateDashboard(data);
        });
    }, function(error) {
        setConnected(false);
        console.error('Erro de conexão STOMP:', error);
        // Tenta reconectar após 5 segundos
        setTimeout(connect, 5000);
    });
}

function setConnected(connected) {
    if (connected) {
        statusBadge.textContent = 'Conectado';
        statusBadge.className = 'badge connected';
    } else {
        statusBadge.textContent = 'Desconectado';
        statusBadge.className = 'badge disconnected';
    }
}

function updateDashboard(data) {
    const cityId = data.cidade.replace(/\s+/g, '-').toLowerCase();
    
    // Define o tema baseado na temperatura (Removendo o verde e dividindo entre frio e calor)
    let themeClass;
    if (data.temperatura <= 25) {
        themeClass = 'cold'; // Azul
    } else {
        themeClass = 'hot'; // Vermelho
    }

    // Check if card already exists
    let card = document.getElementById(`card-${cityId}`);

    if (card) {
        // Update existing card
        card.className = `card ${themeClass}`;
        
        // Remove animation class and re-add it to trigger animation again
        card.classList.remove('updated');
        void card.offsetWidth; // trigger reflow
        card.classList.add('updated');

        card.querySelector('.temperature').textContent = `${data.temperatura.toFixed(1)}°C`;
        card.querySelector('.description').textContent = data.descricao;
        card.querySelector('.time').textContent = data.horario;
        
    } else {
        // Create new card
        card = document.createElement('div');
        card.id = `card-${cityId}`;
        card.className = `card ${themeClass}`;
        
        card.innerHTML = `
            <div class="card-header">
                <span class="city-name">${data.cidade}</span>
                <span class="time">${data.horario}</span>
            </div>
            <div class="temp-container">
                <div class="temperature">${data.temperatura.toFixed(1)}°C</div>
            </div>
            <div class="description">${data.descricao}</div>
        `;
        
        cardsContainer.insertBefore(card, cardsContainer.firstChild);

        // Keep maximum 10 cards on screen
        activeCities.set(cityId, card);
        if (activeCities.size > 10) {
            const oldestId = activeCities.keys().next().value;
            const oldestCard = document.getElementById(`card-${oldestId}`);
            if (oldestCard) {
                oldestCard.remove();
            }
            activeCities.delete(oldestId);
        }
    }
}

// Inicializa a conexão
connect();
