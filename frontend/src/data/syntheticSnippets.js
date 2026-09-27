export const SYNTHETIC_SNIPPETS = [
  {
    id: "snippet-fi-1",
    title: "Order Processing Service (Field Injection)",
    category: "field_injection",
    categoryLabel: "Field Injection",
    description: "Uses @Autowired directly on private non-final fields instead of constructor injection.",
    code: `package com.example.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.repo.OrderRepository;
import com.example.repo.PaymentGateway;

@Service
public class OrderProcessorService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentGateway paymentGateway;

    public void processOrder(Long orderId) {
        var order = orderRepository.findById(orderId);
        paymentGateway.charge(order);
    }
}`,
    typicalRuleKey: "field_injection",
    exampleOverrideNote: "Team convention: Field injection permitted in legacy payment adapters"
  },
  {
    id: "snippet-fi-2",
    title: "User Notification Manager (Field Injection)",
    category: "field_injection",
    categoryLabel: "Field Injection",
    description: "Legacy notification manager injecting multiple email/sms client services via field injection.",
    code: `package com.example.notification;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NotificationManager {

    @Autowired
    private EmailDispatcher emailDispatcher;

    @Autowired
    private SmsGateway smsGateway;

    public void notifyUser(String userId, String message) {
        emailDispatcher.send(userId, message);
        smsGateway.dispatch(userId, message);
    }
}`,
    typicalRuleKey: "field_injection",
    exampleOverrideNote: "Legacy notification components use field injection for backward compatibility"
  },
  {
    id: "snippet-fi-3",
    title: "Inventory Audit Listener (Field Injection)",
    category: "field_injection",
    categoryLabel: "Field Injection",
    description: "Audit event listener autowiring audit loggers and metrics publisher directly.",
    code: `package com.example.audit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuditEventListener {

    @Autowired
    private AuditLogger auditLogger;

    @Autowired
    private MetricRegistry metricRegistry;

    public void recordStockMovement(Long itemId, int delta) {
        auditLogger.logEvent("STOCK_MOVE", itemId, delta);
        metricRegistry.counter("inventory.moves").inc();
    }
}`,
    typicalRuleKey: "field_injection",
    exampleOverrideNote: "Internal audit listeners standardized on field injection"
  },
  {
    id: "snippet-np-1",
    title: "Customer Invoices Fetcher (N+1 Query in Loop)",
    category: "n_plus_one",
    categoryLabel: "N+1 Query Loop",
    description: "Iterating over customer entities and fetching their invoices one by one in a loop.",
    code: `package com.example.billing;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;

@Service
public class InvoiceAggregator {

    private final CustomerRepository customerRepo;
    private final InvoiceRepository invoiceRepo;

    public InvoiceAggregator(CustomerRepository customerRepo, InvoiceRepository invoiceRepo) {
        this.customerRepo = customerRepo;
        this.invoiceRepo = invoiceRepo;
    }

    public List<Invoice> getPendingInvoicesForActiveCustomers() {
        List<Customer> active = customerRepo.findActiveCustomers();
        List<Invoice> result = new ArrayList<>();
        for (Customer c : active) {
            List<Invoice> invoices = invoiceRepo.findByCustomerId(c.getId());
            result.addAll(invoices);
        }
        return result;
    }
}`,
    typicalRuleKey: "n_plus_one",
    exampleOverrideNote: "Customer counts are capped at <= 5 by pagination; batching is handled downstream"
  },
  {
    id: "snippet-np-2",
    title: "Catalog Product Review Loader (N+1 Query)",
    category: "n_plus_one",
    categoryLabel: "N+1 Query Loop",
    description: "Queries product ratings and user details inside a catalog loop.",
    code: `package com.example.catalog;

import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class ProductReviewAggregator {

    private final ProductRepository productRepo;
    private final ReviewRepository reviewRepo;

    public ProductReviewAggregator(ProductRepository p, ReviewRepository r) {
        this.productRepo = p;
        this.reviewRepo = r;
    }

    public Map<Long, Double> calculateAverageRatings(List<Long> productIds) {
        Map<Long, Double> ratings = new HashMap<>();
        for (Long id : productIds) {
            List<Review> reviews = reviewRepo.findByProductId(id);
            double avg = reviews.stream().mapToInt(Review::getStars).average().orElse(0.0);
            ratings.put(id, avg);
        }
        return ratings;
    }
}`,
    typicalRuleKey: "n_plus_one",
    exampleOverrideNote: "ReviewRepo utilizes a Redis L2 cache layer per ID; direct query is fine"
  },
  {
    id: "snippet-np-3",
    title: "Organization Member Hierarchy (N+1 Query)",
    category: "n_plus_one",
    categoryLabel: "N+1 Query Loop",
    description: "Fetches permission sets in a loop over team members.",
    code: `package com.example.org;

import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class TeamPermissionService {

    private final DepartmentRepository deptRepo;
    private final PermissionRepository permRepo;

    public TeamPermissionService(DepartmentRepository d, PermissionRepository p) {
        this.deptRepo = d;
        this.permRepo = p;
    }

    public Set<String> getPermissionsForDept(Long deptId) {
        List<TeamMember> members = deptRepo.findMembers(deptId);
        Set<String> allPerms = new HashSet<>();
        for (TeamMember member : members) {
            PermissionSet perms = permRepo.findByUserId(member.getUserId());
            allPerms.addAll(perms.getScopes());
        }
        return allPerms;
    }
}`,
    typicalRuleKey: "n_plus_one",
    exampleOverrideNote: "Departments are strictly constrained to 3-5 users; individual queries are fine"
  },
  {
    id: "snippet-nc-1",
    title: "Tenant Header Resolver (Missing Null Check)",
    category: "null_check",
    categoryLabel: "Missing Null Checks",
    description: "Extracts tenant header and invokes string trimming without checking for null.",
    code: `package com.example.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class TenantContextResolver {

    public String resolveTenantId(HttpServletRequest request) {
        String header = request.getHeader("X-Tenant-ID");
        return header.trim().toLowerCase();
    }
}`,
    typicalRuleKey: "null_check",
    exampleOverrideNote: "X-Tenant-ID is already verified by API Gateway before reaching backend"
  },
  {
    id: "snippet-nc-2",
    title: "User Profile Formatter (Missing Null Check)",
    category: "null_check",
    categoryLabel: "Missing Null Checks",
    description: "Directly invokes methods on nested address object without checking nullness.",
    code: `package com.example.profile;

public class UserProfileFormatter {

    public String formatShippingAddress(UserProfile profile) {
        Address address = profile.getAddress();
        return address.getStreet().toUpperCase() + ", " + address.getCity() + " " + address.getZipCode();
    }
}`,
    typicalRuleKey: "null_check",
    exampleOverrideNote: "Database constraints and Bean Validation guarantee profile address is non-null"
  },
  {
    id: "snippet-nc-3",
    title: "Payment Metadata Extractor (Missing Null Check)",
    category: "null_check",
    categoryLabel: "Missing Null Checks",
    description: "Retrieves currency code from payment request map without null check.",
    code: `package com.example.payment;

import java.util.Map;

public class PaymentMetadataExtractor {

    public String extractCurrency(Map<String, Object> payload) {
        Object curr = payload.get("currency");
        return curr.toString().trim().toUpperCase();
    }
}`,
    typicalRuleKey: "null_check",
    exampleOverrideNote: "Strict JSON Schema validation at edge ensures currency is non-null"
  },
  {
    id: "snippet-ec-1",
    title: "Cache Eviction Worker (Empty Catch Block)",
    category: "empty_catch",
    categoryLabel: "Swallowed Exception",
    description: "Silently catches Exception during background cache eviction with empty body.",
    code: `package com.example.cache;

import org.springframework.stereotype.Service;

@Service
public class CacheJanitorService {

    private final CacheClient cacheClient;

    public CacheJanitorService(CacheClient cacheClient) {
        this.cacheClient = cacheClient;
    }

    public void purgeStaleEntries(String keyPrefix) {
        try {
            cacheClient.evictKeysStartingWith(keyPrefix);
        } catch (Exception e) {
            // ignore
        }
    }
}`,
    typicalRuleKey: "empty_catch",
    exampleOverrideNote: "Background cache eviction is best-effort; failures must not fail requests"
  },
  {
    id: "snippet-ec-2",
    title: "Temp File Cleaner (Swallowed Exception)",
    category: "empty_catch",
    categoryLabel: "Swallowed Exception",
    description: "Catches IOException when deleting temporary upload files without logging stack trace.",
    code: `package com.example.storage;

import java.io.File;

public class TempFileCleaner {

    public void deleteQuietly(File tempFile) {
        try {
            if (tempFile.exists()) {
                tempFile.delete();
            }
        } catch (Exception ex) {
            // do nothing
        }
    }
}`,
    typicalRuleKey: "empty_catch",
    exampleOverrideNote: "Quiet deletion is intentional for transient upload artifacts"
  },
  {
    id: "snippet-ec-3",
    title: "Heartbeat Ping Task (Empty Catch)",
    category: "empty_catch",
    categoryLabel: "Swallowed Exception",
    description: "Swallows socket timeouts in recurring telemetry heartbeat pings.",
    code: `package com.example.telemetry;

import java.net.Socket;

public class HeartbeatProbe {

    public boolean isServiceAlive(String host, int port) {
        try (Socket s = new Socket(host, port)) {
            return true;
        } catch (Exception ignored) {
        }
        return false;
    }
}`,
    typicalRuleKey: "empty_catch",
    exampleOverrideNote: "Socket failure in probe intentionally returns boolean without error logging"
  },
  {
    id: "snippet-mn-1",
    title: "Session Expiry Calculator (Magic Numbers)",
    category: "magic_number",
    categoryLabel: "Magic Numbers",
    description: "Hardcoded 86400 and 1000 in millisecond calculation.",
    code: `package com.example.auth;

import java.time.Instant;

public class SessionExpiryCalculator {

    public Instant computeExpiration(Instant loginTime, boolean rememberMe) {
        if (rememberMe) {
            return loginTime.plusSeconds(86400 * 30);
        }
        return loginTime.plusSeconds(3600);
    }
}`,
    typicalRuleKey: "magic_number",
    exampleOverrideNote: "Seconds per day (86400) and hour (3600) are standard self-evident constants"
  },
  {
    id: "snippet-mn-2",
    title: "Rate Limiter Token Bucket (Magic Numbers)",
    category: "magic_number",
    categoryLabel: "Magic Numbers",
    description: "Hardcoded capacity 5000 and refill rates in throttling logic.",
    code: `package com.example.gateway;

public class RateLimiterBucket {

    private int tokens = 5000;

    public boolean allowRequest(int cost) {
        if (tokens >= cost) {
            tokens -= cost;
            return true;
        }
        return false;
    }
}`,
    typicalRuleKey: "magic_number",
    exampleOverrideNote: "Default token bucket size is fixed across tier-1 edge clusters"
  },
  {
    id: "snippet-mn-3",
    title: "Buffer Chunk Streamer (Magic Numbers)",
    category: "magic_number",
    categoryLabel: "Magic Numbers",
    description: "1024 byte chunk size embedded directly in packet splitting algorithm.",
    code: `package com.example.stream;

import java.io.InputStream;
import java.io.OutputStream;

public class StreamPumper {

    public void transfer(InputStream in, OutputStream out) throws Exception {
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) != -1) {
            out.write(buf, 0, len);
        }
    }
}`,
    typicalRuleKey: "magic_number",
    exampleOverrideNote: "1024-byte chunk size is the accepted standard socket buffer across modules"
  },
  {
    id: "snippet-tr-1",
    title: "CSV Report Generator (Missing Try-with-Resources)",
    category: "try_with_resources",
    categoryLabel: "Resource Leak / Streams",
    description: "FileWriter and BufferedReader created without try-with-resources statement.",
    code: `package com.example.report;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class ConfigFileReader {

    public String readFirstLine(String path) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(path));
        String line = reader.readLine();
        reader.close();
        return line;
    }
}`,
    typicalRuleKey: "try_with_resources",
    exampleOverrideNote: "Legacy I/O wrapper has custom cleanup hooks managed in teardown"
  },
  {
    id: "snippet-tr-2",
    title: "Database Connection Query Runner (Missing Try-with-Resources)",
    category: "try_with_resources",
    categoryLabel: "Resource Leak / Streams",
    description: "JDBC Connection and Statement opened without try-with-resources block.",
    code: `package com.example.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;

public class RawQueryRunner {

    public int countUsers(String url) throws Exception {
        Connection conn = DriverManager.getConnection(url);
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT count(*) FROM users");
        rs.next();
        int count = rs.getInt(1);
        conn.close();
        return count;
    }
}`,
    typicalRuleKey: "try_with_resources",
    exampleOverrideNote: "Connection pooling wrapper automatically cleans up connection on return"
  },
  {
    id: "snippet-tr-3",
    title: "Binary Asset Stream Copy (Missing Try-with-Resources)",
    category: "try_with_resources",
    categoryLabel: "Resource Leak / Streams",
    description: "FileInputStream opened and read without try-with-resources block.",
    code: `package com.example.media;

import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;

public class AssetReader {

    public byte[] loadBytes(String filepath) throws Exception {
        FileInputStream fis = new FileInputStream(filepath);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int read;
        while ((read = fis.read(buf)) != -1) {
            baos.write(buf, 0, read);
        }
        fis.close();
        return baos.toByteArray();
    }
}`,
    typicalRuleKey: "try_with_resources",
    exampleOverrideNote: "Native asset loaders are lifecycle-managed by the native image runtime"
  }
];
