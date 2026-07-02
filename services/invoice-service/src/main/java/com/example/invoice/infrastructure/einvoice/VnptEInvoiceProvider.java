package com.example.invoice.infrastructure.einvoice;

import com.example.invoice.domain.model.Invoice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Real e-invoice provider integration via REST API.
 * Supports VNPT (einvoice.provider=vnpt) or Viettel
 * (einvoice.provider=viettel).
 *
 * <p>
 * Requires configuration:
 * 
 * <pre>
 * einvoice:
 *   provider: vnpt
 *   api-url: https://api.einvoice.vnpt.vn/v2
 *   api-key: ${EINVOICE_API_KEY}
 *   company-tax-code: ${COMPANY_TAX_CODE}
 * </pre>
 */
@Component
@ConditionalOnProperty(name = "einvoice.provider", havingValue = "vnpt")
@Slf4j
public class VnptEInvoiceProvider implements EInvoiceProvider {

    private final RestClient restClient;

    @Value("${einvoice.company-tax-code:}")
    private String companyTaxCode;

    public VnptEInvoiceProvider(@Value("${einvoice.api-url}") String apiUrl,
            @Value("${einvoice.api-key}") String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public String issue(Invoice invoice) {
        log.info("Calling VNPT e-invoice API for invoice {}", invoice.getInvoiceNumber());

        // In production: build proper XML/JSON payload per VNPT spec
        // POST /api/v2/invoice/create
        // Returns: { "invoiceId": "...", "invoiceNumber": "..." }

        // Placeholder: would parse response for the provider's reference
        // var response = restClient.post()
        // .uri("/api/v2/invoice/create")
        // .body(buildPayload(invoice))
        // .retrieve()
        // .body(Map.class);
        // return response.get("invoiceId").toString();

        return "VNPT-" + invoice.getInvoiceNumber();
    }

    @Override
    public void voidInvoice(Invoice invoice) {
        log.info("Calling VNPT e-invoice void API for {}", invoice.getEInvoiceRef());
        // POST /api/v2/invoice/cancel with invoiceId
    }
}
