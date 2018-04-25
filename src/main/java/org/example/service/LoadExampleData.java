package org.example.service;

import io.ebean.Ebean;
import io.ebean.EbeanServer;
import io.ebean.annotation.Transactional;
import io.ebean.annotation.TxType;
import org.example.domain.Address;
import org.example.domain.Contact;
import org.example.domain.Country;
import org.example.domain.Customer;
import org.example.domain.Order;
import org.example.domain.Order.Status;
import org.example.domain.OrderDetail;
import org.example.domain.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class LoadExampleData {

  private static final Logger log = LoggerFactory.getLogger(LoadExampleData.class);

  private static boolean runOnce;

  private static EbeanServer server = Ebean.getServer(null);

  public static synchronized void load() {

    if (runOnce) {
      return;
    }
    new LoadExampleData().loadAll();
  }

  public synchronized void loadAll() {
    runOnce = true;
    if (Country.find.query().findCount() > 0) {
      return;
    }
    deleteAll();
    insertReference();
    insertTestCustAndOrders();
  }

  @Transactional(type = TxType.REQUIRES_NEW)
  private void insertReference() {
    insertCountries();
    insertProducts();
  }


  //@Transactional(type = TxType.REQUIRES_NEW)
  private void deleteAll() {
    Ebean.execute(() -> {

      // Ebean.currentTransaction().setBatchMode(false);

      // orm update use bean name and bean properties
      // server.createUpdate(OrderShipment.class, "delete from orderShipment").execute();

      server.createUpdate(OrderDetail.class, "delete from orderDetail").execute();
      server.createUpdate(Order.class, "delete from order").execute();
      server.createUpdate(Contact.class, "delete from contact").execute();
      server.createUpdate(Customer.class, "delete from Customer").execute();
      server.createUpdate(Address.class, "delete from address").execute();

      // sql update uses table and column names
      server.createSqlUpdate("delete from country").execute();
      server.createSqlUpdate("delete from product").execute();
    });
  }

  private void insertCountries() {

    server.execute(() ->  {
      new Country("NZ", "New Zealand").save();
      new Country("AU", "Australia").save();
    });
  }

  private List<Product> products = new ArrayList<>();

  private void insertProducts() {

    products.add(new Product("C001", "Chair"));
    products.add(new Product("DSK1", "Desk"));
    products.add(new Product("C002", "Computer"));
    products.add(new Product("C003", "Printer"));

    server.saveAll(products);
  }

  private void insertTestCustAndOrders() {

    Ebean.execute( () -> {
        Customer cust1 = insertCustomer("Rob");
        Customer cust2 = insertCustomerNoAddress();
        insertCustomerFiona();
        insertCustomerNoContacts("NocCust");

        createOrder1(cust1);
        createOrder2(cust2);
        createOrder3(cust1);
        createOrder4(cust1);
      }
    );
  }

  public static Customer createCustAndOrder(String custName) {

    LoadExampleData me = new LoadExampleData();
    Customer cust1 = insertCustomer(custName);
    me.createOrder1(cust1);
    return cust1;
  }

  public static Order createOrderCustAndOrder(String custName) {

    LoadExampleData me = new LoadExampleData();
    Customer cust1 = insertCustomer(custName);
    Order o = me.createOrder1(cust1);
    return o;
  }

  private static int contactEmailNum = 1;

  private Customer insertCustomerFiona() {

    Customer c = createCustomer("Fiona", "12 Apple St", "West Coast Rd", 1);

    c.addContact(createContact("Fiona", "Black"));
    c.addContact(createContact("Tracy", "Red"));

    Ebean.save(c);
    return c;
  }

  public static Contact createContact(String firstName, String lastName) {
    Contact contact = new Contact();
    contact.setFirstName(firstName);
    contact.setLastName(lastName);
    String email = contact.getLastName() + (contactEmailNum++) + "@test.com";
    contact.setEmail(email.toLowerCase());
    return contact;
  }

  private Customer insertCustomerNoContacts(String name) {

    Customer c = createCustomer(name, "15 Kumera Way", "Bos town", 1);

    Ebean.save(c);
    return c;
  }

  private Customer insertCustomerNoAddress() {

    Customer c = new Customer("Jack Hill");
    c.addContact(createContact("Jack", "Black"));
    c.addContact(createContact("Jill", "Hill"));
    c.addContact(createContact("Mac", "Hill"));

    Ebean.save(c);
    return c;
  }

  private static Customer insertCustomer(String name) {
    Customer c = createCustomer(name, "1 Banana St", "P.O.Box 1234", 1);
    Ebean.save(c);
    return c;
  }

  private static Customer createCustomer(String name, String shippingStreet, String billingStreet, int contactSuffix) {

    Customer c = new Customer(name);
    if (contactSuffix > 0) {
      c.addContact(new Contact("Jim" + contactSuffix, "Cricket"));
      c.addContact(new Contact("Fred" + contactSuffix, "Blue"));
      c.addContact(new Contact("Bugs" + contactSuffix, "Bunny"));
    }

    if (shippingStreet != null) {
      Address shippingAddr = new Address();
      shippingAddr.setLine1(shippingStreet);
      shippingAddr.setLine2("Sandringham");
      shippingAddr.setCity("Auckland");
      shippingAddr.setCountry(Country.find.ref("NZ"));

      c.setShippingAddress(shippingAddr);
    }

    if (billingStreet != null) {
      Address billingAddr = new Address();
      billingAddr.setLine1(billingStreet);
      billingAddr.setLine2("St Lukes");
      billingAddr.setCity("Auckland");
      billingAddr.setCountry(Ebean.getReference(Country.class, "NZ"));

      c.setBillingAddress(billingAddr);
    }

    return c;
  }

  private Order createOrder1(Customer customer) {

    // these id values are not predicable depending on DB Id type (Cockroach serial)
    Product product1 = Product.find.ref(products.get(0).getId());
    Product product2 = Product.find.ref(products.get(1).getId());
    Product product3 = Product.find.ref(products.get(2).getId());

    Order order = new Order(customer);

    List<OrderDetail> details = new ArrayList<>();
    details.add(new OrderDetail(product1, 5, 10.50));
    details.add(new OrderDetail(product2, 3, 1.10));
    details.add(new OrderDetail(product3, 1, 2.00));
    order.setDetails(details);

    //order.addShipment(new OrderShipment());

    Ebean.save(order);
    return order;
  }

  private void createOrder2(Customer customer) {

    Product product1 = Product.find.ref(products.get(0).getId());

    Order order = new Order(customer);
    order.setStatus(Status.SHIPPED);
    order.setShipDate(LocalDate.now().plusDays(1));

    List<OrderDetail> details = new ArrayList<>();
    details.add(new OrderDetail(product1, 4, 10.50));
    order.setDetails(details);

    //order.addShipment(new OrderShipment());

    Ebean.save(order);
  }

  private void createOrder3(Customer customer) {

    Product product0 = Product.find.ref(products.get(0).getId());
    Product product3 = Product.find.ref(products.get(3).getId());


    Order order = new Order(customer);
    order.setStatus(Status.COMPLETE);
    order.setShipDate(LocalDate.now().plusDays(2));

    List<OrderDetail> details = new ArrayList<>();
    details.add(new OrderDetail(product0, 3, 10.50));
    details.add(new OrderDetail(product3, 40, 2.10));
    order.setDetails(details);

    //order.addShipment(new OrderShipment());

    Ebean.save(order);
  }

  private void createOrder4(Customer customer) {

    Order order = new Order(customer);
    Ebean.save(order);
  }
}
