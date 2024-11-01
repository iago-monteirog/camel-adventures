package br.com.caelum.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http4.HttpMethods;
import org.apache.camel.impl.DefaultCamelContext;


public class RotaPedidos {

    public static void main(String[] args) throws Exception {

        CamelContext context = new DefaultCamelContext();
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("file:pedidos?delay=5s&noop=true")
                        .routeId("rotaPedidos")
                        .multicast()
                        .to("direct:http")
                        .to("direct:soap");


                        from("direct:http")
                                .routeId("rotaHttp")
                            .setProperty("pedidoId", xpath("/pedido/id/text()"))
                            .setProperty("clientId", xpath("/pedido/pagamento/email-titular/text()"))
                            .split()
                            .xpath("/pedido/itens/item")
                            .log("${id}")
                            .filter()
                            .xpath("/item/formato[text()='EBOOK']")
                            .setProperty("ebookId", xpath("/item/livro/codigo/text()"))
                            .marshal().xmljson()
                            .log("${body}")
                            .setHeader(Exchange.HTTP_METHOD, HttpMethods.GET)
                            .setHeader(Exchange.HTTP_QUERY,
                                    simple("ebookId=${property.ebookId}&pedidoId=${property.pedidoId}&clienteId=${property.clientId}"))
                            .to("http4://localhost:8080/webservices/ebook/item");

                        from("direct:soap")
                                .routeId("rotaSoap")
                                .setBody(constant("<envolope>teste</envelope>"))
                        .to("mock:soap");
            }
        });

        context.start();
        Thread.sleep(20000);
        context.stop();

    }
}
