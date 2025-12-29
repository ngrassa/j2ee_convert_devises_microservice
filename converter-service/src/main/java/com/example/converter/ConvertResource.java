package com.example.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

@Path("/convert")
@Produces(MediaType.APPLICATION_JSON)
public class ConvertResource {
    private static final Logger LOGGER = Logger.getLogger(ConvertResource.class.getName());
    private static final String DEFAULT_RATES_URL = "http://localhost:8081/api";
    private static final String EXTERNAL_RATE_API =
            System.getenv().getOrDefault("EXTERNAL_RATE_API", "").trim();
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @GET
    public Response convert(
            @QueryParam("amount") Double amount,
            @QueryParam("from") String from,
            @QueryParam("to") String to) {
        if (amount == null || from == null || to == null || from.isBlank() || to.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "amount, from, to sont requis"))
                    .build();
        }

        if (!EXTERNAL_RATE_API.isEmpty()) {
            Response externalResponse = convertUsingExternal(amount, from, to);
            if (externalResponse != null) {
                return externalResponse;
            }
            LOGGER.warning("External rates missing data; falling back to internal rates");
        }

        String baseUrl = System.getenv().getOrDefault("RATES_SERVICE_URL", DEFAULT_RATES_URL);
        String url = baseUrl + "/rates";
        LOGGER.info(() -> "Fetching rates from " + url);
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return Response.status(Response.Status.BAD_GATEWAY)
                        .entity(Map.of("error", "Failed to fetch rates", "status", response.statusCode()))
                        .build();
            }
            List<Rate> rates = mapper.readValue(response.body(), new TypeReference<List<Rate>>() {});
            Optional<Rate> fromRate = findRate(rates, from);
            Optional<Rate> toRate = findRate(rates, to);
            if (fromRate.isEmpty() || toRate.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Devise inconnue (from/to)"))
                        .build();
            }

            if (fromRate.get().currency().equalsIgnoreCase(toRate.get().currency())) {
                return Response.ok(Map.of(
                        "from", fromRate.get().currency(),
                        "to", toRate.get().currency(),
                        "amount", amount,
                        "converted", amount,
                        "rateFrom", fromRate.get().rate(),
                        "rateTo", toRate.get().rate()
                )).build();
            }

            double amountInTnd = amount;
            if (!fromRate.get().currency().equalsIgnoreCase("TND")) {
                amountInTnd = amount / fromRate.get().rate();
            }
            double converted = amountInTnd;
            if (!toRate.get().currency().equalsIgnoreCase("TND")) {
                converted = amountInTnd * toRate.get().rate();
            }

            return Response.ok(Map.of(
                    "from", fromRate.get().currency(),
                    "to", toRate.get().currency(),
                    "amount", amount,
                    "converted", converted,
                    "rateFrom", fromRate.get().rate(),
                    "rateTo", toRate.get().rate()
            )).build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Response.serverError()
                    .entity(Map.of("error", "Conversion interrupted"))
                    .build();
        } catch (IOException e) {
            return Response.serverError()
                    .entity(Map.of("error", "Conversion failed: " + e.getMessage()))
                    .build();
        }
    }

    private Optional<Rate> findRate(List<Rate> rates, String code) {
        return rates.stream()
                .filter(r -> r.currency().equalsIgnoreCase(code))
                .findFirst();
    }

    private Response convertUsingExternal(Double amount, String from, String to) {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(EXTERNAL_RATE_API)).GET().build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return null;
            }
            ExternalRates external = mapper.readValue(response.body(), ExternalRates.class);
            String base = external.base().toUpperCase();
            Map<String, Double> rates = external.rates();
            if (base == null || rates == null || rates.isEmpty()) {
                return null;
            }

            Double rateFrom = rateAgainstBase(base, rates, from);
            Double rateTo = rateAgainstBase(base, rates, to);
            if (rateFrom == null || rateTo == null) {
                return null;
            }
            double converted = amount * (rateTo / rateFrom);
            return Response.ok(Map.of(
                    "from", from.toUpperCase(),
                    "to", to.toUpperCase(),
                    "amount", amount,
                    "converted", converted,
                    "rateFrom", rateFrom,
                    "rateTo", rateTo,
                    "base", base,
                    "source", "external"
            )).build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    private Double rateAgainstBase(String base, Map<String, Double> rates, String code) {
        String key = code.toUpperCase();
        if (key.equals(base)) {
            return 1.0;
        }
        return rates.get(key);
    }

    public record ExternalRates(String base, Map<String, Double> rates) {}
}
