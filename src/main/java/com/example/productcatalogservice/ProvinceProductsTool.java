package com.example.productcatalogservice;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class ProvinceProductsTool {
    private static final Map<String, Region> REGIONS = Map.ofEntries(
        Map.entry("on", new Region("Ontario")),
        Map.entry("qc", new Region("Quebec")),
        Map.entry("ab", new Region("Alberta")),
        Map.entry("bc", new Region("British Columbia")),
        Map.entry("mb", new Region("Manitoba")),
        Map.entry("nb", new Region("New Brunswick")),
        Map.entry("nl", new Region("Newfoundland and Labrador")),
        Map.entry("ns", new Region("Nova Scotia")),
        Map.entry("pe", new Region("Prince Edward Island")),
        Map.entry("sk", new Region("Saskatchewan")),
        Map.entry("nt", new Region("Northwest Territories")),
        Map.entry("yt", new Region("Yukon")),
        Map.entry("nu", new Region("Nunavut"))
    );

    // @Tool(description = "Find sample internet plans for a Canadian province or territory by name or two-letter code. Nunavut has no service.")
    @Tool(description = """
        Find sample internet plans for a Canadian province or territory.
        Accept province names or two-letter codes.
        If the user provides a city, identify its province first.
        Example: Montreal → Quebec, Toronto → Ontario.
        Nunavut has no service.
        Return the tool result as JSON without modifying its structure or values.
        """, returnDirect = true)
    public ProvinceProducts getProvinceProducts(@ToolParam(description = "Canadian province or territory name or two-letter code") String province) {
        if (province == null || province.isBlank()) throw new IllegalArgumentException("province is required");
        String input = normalize(province);
        var entry = REGIONS.entrySet().stream()
            .filter(e -> e.getKey().equals(input) || normalize(e.getValue().name()).equals(input))
            .findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown Canadian province or territory"));
        String code = entry.getKey();
        String name = entry.getValue().name();
        if (code.equals("nu")) return new ProvinceProducts(name, "No service available", List.of());
        String firstName = switch (code) {
            case "on" -> "Ontario Super Save 1Gbps";
            case "qc" -> "French Frenzy 5Gbps";
            default -> name + " Super Save 1Gbps";
        };
        int firstSpeed = code.equals("qc") ? 5000 : 1000;
        var plans = List.of(
            plan(code + "-super", firstName, firstSpeed, "79.99"),
            plan(code + "-essential", name + " Essential 500Mbps", 500, "59.99"),
            plan(code + "-basic", name + " Basic 100Mbps", 100, "39.99")
        );
        return new ProvinceProducts(name, "Plans available", plans);
    }

    private static String normalize(String input) {
        return Normalizer.normalize(input.strip(), Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "").replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static Product plan(String id, String name, int speed, String price) {
        return new Product(id, name, "Sample home internet plan", speed, new BigDecimal(price), "CAD");
    }

    private record Region(String name) {}
    public record ProvinceProducts(String province, String message, List<Product> products) {}
}
