package com.example.rates;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Path("/rates")
@Produces(MediaType.APPLICATION_JSON)
public class RateResource {
    private static final Map<String, Double> RATES;

    static {
        Map<String, Double> map = new LinkedHashMap<>();
        map.put("TND", 1.0);
        map.put("EUR", 0.29);
        map.put("USD", 0.31);
        map.put("GBP", 0.25);
        map.put("JPY", 46.0);
        map.put("CAD", 0.42);
        RATES = Collections.unmodifiableMap(map);
    }

    @GET
    public List<Rate> listAll() {
        List<Rate> rates = new ArrayList<>();
        RATES.forEach((currency, rate) -> rates.add(new Rate(currency, rate)));
        return rates;
    }

    @GET
    @Path("/{currency}")
    public Response findByCurrency(@PathParam("currency") String currency) {
        Double rate = RATES.get(currency.toUpperCase());
        if (rate == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Unsupported currency: " + currency))
                    .build();
        }
        return Response.ok(new Rate(currency.toUpperCase(), rate)).build();
    }
}
