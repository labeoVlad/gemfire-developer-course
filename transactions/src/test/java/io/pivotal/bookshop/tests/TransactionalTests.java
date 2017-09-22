package io.pivotal.bookshop.tests;

import io.pivotal.bookshop.buslogic.TransactionalService;
import io.pivotal.bookshop.domain.Address;
import io.pivotal.bookshop.domain.Customer;
import io.pivotal.bookshop.domain.Order;
import io.pivotal.bookshop.domain.ProductItem;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.TransactionException;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientCacheFactory;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// TODO-01: Open the test harness and observe the two transaction tests to be run
// TODO-07: Ensure the locator & servers have been started, then run the tests, verifying both tests pass.
// TODO-11: Stop & restart the locator & servers and re-run the tests. Verify the output of the listeners.log file
public class TransactionalTests {
  private ClientCache cache;
  private Region<Long, Order> orderRegion;
  private Region<Long, Customer> customerRegion;

  @Before
  public void setUp() throws Exception {
    this.cache = new ClientCacheFactory().create();
    orderRegion = cache.getRegion("Order");
    customerRegion = cache.getRegion("Customer");

    // Force the region data to be removed so log will only show true
    // updates. The region.clear() method doesn't
    // work for clients calling clear() on partitioned regions.
    orderRegion.removeAll(orderRegion.keySetOnServer());
    customerRegion.removeAll(customerRegion.keySetOnServer());

    loadCustomers();
    loadOrders();
  }

  @Test
  public void shouldCommitTransaction() {
    TransactionalService svc = new TransactionalService(cache);

    // Parametrized data for test
    long customerKey = 1001;
    long orderKey = 1001;
    Calendar updatedOrderDate = Calendar.getInstance();
    updatedOrderDate.set(2013, Calendar.APRIL, 25, 0, 0, 0);
    String updatedCustomerPhone = "222-22222-0000";

    svc.updateCustomerAndOrder(customerKey, orderKey, updatedCustomerPhone, updatedOrderDate.getTime());

    // Verify that changes were made in both regions
    Customer cust = customerRegion.get(customerKey);
    Order order = orderRegion.get(orderKey);

    assertThat(cust.getPhoneNumber()).isEqualTo(updatedCustomerPhone);
    assertThat(order.getOrderDate()).isEqualTo(updatedOrderDate.getTime());
  }

  @Test(expected = TransactionException.class)
  public void shouldRollbackTransaction() {
    TransactionalService svc = new TransactionalService(cache);

    // Parametrized data for test
    long customerKey = 1001;
    long orderKey = 1002;
    Calendar updatedOrderDate = Calendar.getInstance();
    updatedOrderDate.set(2013, Calendar.APRIL, 25, 0, 0, 0);
    String updatedCustomerPhone = "222-22222-0000";

    svc.updateCustomerAndOrder(customerKey, orderKey, updatedCustomerPhone, updatedOrderDate.getTime());
  }

  private void loadOrders() {
    Map<Long, Order> ordersMap = new HashMap<>();

    Calendar orderDate = Calendar.getInstance();
    orderDate.set(2013, Calendar.DECEMBER, 3, 0, 0, 0);
    ArrayList<ProductItem> arr1 = new ArrayList<>();
    arr1.add(new ProductItem("P001", "Toy", 30.5f));
    arr1.add(new ProductItem("P002", "Watch", 60.5f));
    arr1.add(new ProductItem("P003", "Pen", 12.5f));
    Order order01 = new Order("ORD001", orderDate.getTime(), arr1, "C001", 103.5f);
    ordersMap.put(1001L, order01);

    ArrayList<ProductItem> arr2 = new ArrayList<>();
    arr2.add(new ProductItem("P004", "Shirt", 60.5f));
    arr2.add(new ProductItem("P005", "Socks", 12.5f));
    Order order02 = new Order("ORD002", orderDate.getTime(), arr2, "C002", 73.0f);
    ordersMap.put(1002L, order02);

    orderRegion.putAll(ordersMap);
  }

  private void loadCustomers() {
    Map<Long, Customer> customerMap = new HashMap<>();
    Address address1 = Address.builder().addressLine1("123 Main St")
        .city("Topeka").state("KS").postalCode("50505")
        .country("US").addressTag("HOME").build();
    Customer cust1 = Customer.builder().customerNumber("C001")
        .firstName("Lula").lastName("Wax").phoneNumber("123 654-543")
        .address(address1).build();
    customerMap.put(1001L, cust1);

    Address address2 = Address.builder().addressLine1("123 Main St")
        .city("San Francisco").state("CA").postalCode("50505")
        .country("US").addressTag("HOME").build();
    Customer cust2 = Customer.builder().customerNumber("C002")
        .firstName("Tom").lastName("Mcginns").phoneNumber("123 456-789")
        .address(address2).build();
    customerMap.put(1002L, cust2);

    Address address3 = Address.builder().addressLine1("123 Main St")
        .city("San Francisco").state("CA").postalCode("50505")
        .country("US").addressTag("HOME").build();
    Customer cust3 = Customer.builder().customerNumber("C003")
        .firstName("Peter").lastName("Fernandez").phoneNumber("123 456-789")
        .address(address3).build();
    customerMap.put(1003L, cust3);

    customerRegion.putAll(customerMap);
  }

}
