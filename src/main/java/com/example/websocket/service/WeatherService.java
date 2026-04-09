package com.example.websocket.service;

import com.example.websocket.model.ClimateData;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class WeatherService {

    private final SimpMessagingTemplate messagingTemplate;
    private final RestTemplate restTemplate;
    private final Random random = new Random();

    // Lista de 10 cidades do Brasil
    private static final List<City> CITIES = Arrays.asList(
            new City("São Paulo", -23.55, -46.63),
            new City("Rio de Janeiro", -22.91, -43.17),
            new City("Brasília", -15.78, -47.93),
            new City("Salvador", -12.97, -38.50),
            new City("Fortaleza", -3.72, -38.54),
            new City("Manaus", -3.10, -60.02),
            new City("Curitiba", -25.43, -49.27),
            new City("Belém", -1.46, -48.50),
            new City("Porto Alegre", -30.03, -51.23),
            new City("Recife", -8.05, -34.88)
    );

    public WeatherService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
        this.restTemplate = new RestTemplate();
    }

    @Scheduled(fixedRate = 5000)
    @SuppressWarnings("unchecked")
    public void enviarClima() {
        City city = CITIES.get(random.nextInt(CITIES.size()));
        String url = String.format("https://api.open-meteo.com/v1/forecast?latitude=%s&longitude=%s&current_weather=true",
                city.lat, city.lon);

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("current_weather")) {
                Map<String, Object> currentWeather = (Map<String, Object>) response.get("current_weather");
                double temp = Double.parseDouble(currentWeather.get("temperature").toString());
                int weatherCode = (int) Double.parseDouble(currentWeather.get("weathercode").toString());

                String descricao = mapWeatherCodeToDescription(weatherCode);
                String horario = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

                ClimateData data = new ClimateData(city.name, temp, descricao, horario);
                
                // Envia para o tópico específico
                messagingTemplate.convertAndSend("/topic/clima", data);
            }
        } catch (Exception e) {
            System.err.println("Erro ao buscar dados da API para " + city.name + ": " + e.getMessage());
        }
    }

    private String mapWeatherCodeToDescription(int code) {
        if (code == 0) return "Céu Limpo";
        if (code == 1 || code == 2 || code == 3) return "Parcialmente Nublado";
        if (code == 45 || code == 48) return "Neblina";
        if (code >= 51 && code <= 55) return "Garoa";
        if (code >= 61 && code <= 65) return "Chuva";
        if (code >= 71 && code <= 77) return "Neve";
        if (code >= 80 && code <= 82) return "Pancadas de Chuva";
        if (code >= 95 && code <= 99) return "Tempestade com Raios";
        return "Desconhecido";
    }

    private static class City {
        String name;
        double lat;
        double lon;

        City(String name, double lat, double lon) {
            this.name = name;
            this.lat = lat;
            this.lon = lon;
        }
    }
}
