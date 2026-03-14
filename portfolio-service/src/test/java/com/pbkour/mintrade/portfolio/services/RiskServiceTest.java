package com.pbkour.mintrade.portfolio.services;

import com.pbkour.mintrade.commons.generators.PriceGenerator;
import com.pbkour.mintrade.commons.orders.RejectionReason;
import com.pbkour.mintrade.commons.orders.Side;
import com.pbkour.mintrade.commons.orders.Symbol;
import com.pbkour.mintrade.portfolio.entities.AccountEntity;
import com.pbkour.mintrade.portfolio.entities.AccountLimitEntity;
import com.pbkour.mintrade.portfolio.entities.PositionEntity;
import com.pbkour.mintrade.portfolio.entities.PositionEntity.PositionId;
import com.pbkour.mintrade.portfolio.repositories.AccountLimitsRepository;
import com.pbkour.mintrade.portfolio.repositories.AccountsRepository;
import com.pbkour.mintrade.portfolio.repositories.PositionsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.StampedLock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskServiceTest {

    @Mock
    private AccountsRepository accountsRepository;
    @Mock
    private AccountLimitsRepository accountLimitsRepository;
    @Mock
    private PositionsRepository positionsRepository;
    @Mock
    private PriceGenerator priceGenerator;

    @InjectMocks
    private RiskService riskService;

    private UUID accountId;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
    }

    @Test
    void accountNotFound_throwsRiskCheckFailed() {
        when(accountsRepository.findById(accountId)).thenReturn(Optional.empty());

        // account lookup throws
        assertThrows(RiskCheckFailedException.class,
            () -> riskService.riskCheck(accountId, Symbol.EURUSD, BigDecimal.ONE, Side.BUY));
    }

    @Test
    void accountLimitNotFound_throwsRiskCheckFailed() {
        AccountEntity account = AccountEntity.builder()
            .id(accountId)
            .equity(new BigDecimal("1000.00"))
            .createdAt(Instant.now())
            .build();

        when(accountsRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accountLimitsRepository.findById(accountId)).thenReturn(Optional.empty());

        // missing account limits throws
        assertThrows(RiskCheckFailedException.class,
            () -> riskService.riskCheck(accountId, Symbol.EURUSD, BigDecimal.ONE, Side.BUY));
    }

    @Test
    void insufficientMargin_throwsRiskCheckFailed() {
        // equity small so available margin will be less than required
        AccountEntity account = AccountEntity.builder()
            .id(accountId)
            .equity(new BigDecimal("100.00"))
            .createdAt(Instant.now())
            .build();

        AccountLimitEntity limit = AccountLimitEntity.builder()
            .accountId(accountId)
            .maxNotional(new BigDecimal("1000"))
            .maxPosPerSymbol(new BigDecimal("1000"))
            .marginRateFx(new BigDecimal("0.1"))
            .marginRateStock(new BigDecimal("0.1"))
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        // positions exist and will contribute to used margin
        PositionEntity pos = PositionEntity.builder()
            .id(new PositionId(accountId, Symbol.EURUSD))
            .netQty(new BigDecimal("5"))
            .avgPrice(new BigDecimal("10.00"))
            .build();

        when(accountsRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accountLimitsRepository.findById(accountId)).thenReturn(Optional.of(limit));
        when(positionsRepository.findByIdAccountId(accountId)).thenReturn(List.of(pos));
        // priceGenerator returns 10 for any symbol
        when(priceGenerator.generatePrice(Symbol.EURUSD)).thenReturn(new BigDecimal("10.00"));

        // requiredMargin = maxNotional * marginRate = 1000 * 0.1 = 100
        // usedMargin = sum(|5| * 10) * 0.1 = 50 * 0.1 = 5
        // availableMargin = 100 - 5 = 95 < 100 -> fail
        RiskService.RiskCheckResult result = riskService.riskCheck(accountId, Symbol.EURUSD, BigDecimal.ZERO, Side.BUY);
        assertFalse(result.allowed());
        assertEquals(RejectionReason.REQUIRED_MARGIN.name(), result.reason());
    }

    @Test
    void maxPositionExceeded_throwsRiskCheckFailed() {
        AccountEntity account = AccountEntity.builder()
            .id(accountId)
            .equity(new BigDecimal("100000.00"))
            .createdAt(Instant.now())
            .build();

        AccountLimitEntity limit = AccountLimitEntity.builder()
            .accountId(accountId)
            .maxNotional(new BigDecimal("100000"))
            .maxPosPerSymbol(new BigDecimal("10"))
            .marginRateFx(new BigDecimal("0.1"))
            .marginRateStock(new BigDecimal("0.1"))
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        // existing position of 8 units for EURUSD
        PositionEntity pos = PositionEntity.builder()
            .id(new PositionId(accountId, Symbol.EURUSD))
            .netQty(new BigDecimal("8"))
            .avgPrice(new BigDecimal("100.00"))
            .build();

        when(accountsRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accountLimitsRepository.findById(accountId)).thenReturn(Optional.of(limit));
        when(positionsRepository.findByIdAccountId(accountId)).thenReturn(List.of(pos));
        when(priceGenerator.generatePrice(Symbol.EURUSD)).thenReturn(new BigDecimal("100.00"));

        // try to add quantity 5 -> new netQty = 13 > maxPosPerSymbol(10)
        RiskService.RiskCheckResult result = riskService.riskCheck(accountId, Symbol.EURUSD, new BigDecimal("5"), Side.BUY);
        assertFalse(result.allowed());
        assertEquals(RejectionReason.POSITION_LIMIT.name(), result.reason());
    }

    @Test
    void riskCheck_success_noException() {
        AccountEntity account = AccountEntity.builder()
            .id(accountId)
            .equity(new BigDecimal("100000.00"))
            .createdAt(Instant.now())
            .build();

        AccountLimitEntity limit = AccountLimitEntity.builder()
            .accountId(accountId)
            .maxNotional(new BigDecimal("100000"))
            .maxPosPerSymbol(new BigDecimal("100"))
            .marginRateFx(new BigDecimal("0.1"))
            .marginRateStock(new BigDecimal("0.1"))
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        when(accountsRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accountLimitsRepository.findById(accountId)).thenReturn(Optional.of(limit));
        // no existing positions
        when(positionsRepository.findByIdAccountId(accountId)).thenReturn(List.of());
        // price needed for order notional calculation
        when(priceGenerator.generatePrice(Symbol.EURUSD)).thenReturn(new BigDecimal("1"));

        RiskService.RiskCheckResult result = riskService.riskCheck(accountId, Symbol.EURUSD, new BigDecimal("10"), Side.BUY);
        assertTrue(result.allowed());
    }

    @Test
    void openingNewPosition_exceedsMaxPosPerSymbol_throwsRiskCheckFailed() {
        AccountEntity account = AccountEntity.builder()
            .id(accountId)
            .equity(new BigDecimal("100000.00"))
            .createdAt(Instant.now())
            .build();

        AccountLimitEntity limit = AccountLimitEntity.builder()
            .accountId(accountId)
            .maxNotional(new BigDecimal("100000"))
            .maxPosPerSymbol(new BigDecimal("10"))
            .marginRateFx(new BigDecimal("0.1"))
            .marginRateStock(new BigDecimal("0.1"))
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        // No existing position for GBPUSD; trying to open with quantity 11 should fail
        when(accountsRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accountLimitsRepository.findById(accountId)).thenReturn(Optional.of(limit));
        when(positionsRepository.findByIdAccountId(accountId)).thenReturn(List.of());
        // price stub for GBPUSD used when computing order notional
        when(priceGenerator.generatePrice(Symbol.GBPUSD)).thenReturn(new BigDecimal("1"));

        RiskService.RiskCheckResult result = riskService.riskCheck(accountId, Symbol.GBPUSD, new BigDecimal("11"), Side.BUY);
        assertFalse(result.allowed());
        assertEquals(RejectionReason.POSITION_LIMIT.name(), result.reason());
    }

    @Test
    void usedMargin_calculatedAcrossMultipleSymbols_andRespectsPrices() {
        AccountEntity account = AccountEntity.builder()
            .id(accountId)
            .equity(new BigDecimal("1000.00"))
            .createdAt(Instant.now())
            .build();

        AccountLimitEntity limit = AccountLimitEntity.builder()
            .accountId(accountId)
            .maxNotional(new BigDecimal("1000"))
            .maxPosPerSymbol(new BigDecimal("1000"))
            .marginRateFx(new BigDecimal("0.1"))
            .marginRateStock(new BigDecimal("0.1"))
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        // existing positions: EURUSD qty=2 @ price 100, AAPL qty=3 @ price 10
        PositionEntity p1 = PositionEntity.builder()
            .id(new PositionId(accountId, Symbol.EURUSD))
            .netQty(new BigDecimal("2"))
            .avgPrice(new BigDecimal("100.00"))
            .build();

        PositionEntity p2 = PositionEntity.builder()
            .id(new PositionId(accountId, Symbol.AAPL))
            .netQty(new BigDecimal("3"))
            .avgPrice(new BigDecimal("10.00"))
            .build();

        when(accountsRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accountLimitsRepository.findById(accountId)).thenReturn(Optional.of(limit));
        when(positionsRepository.findByIdAccountId(accountId)).thenReturn(List.of(p1, p2));

        // price generator must be called with corresponding symbols and return the configured prices
        when(priceGenerator.generatePrice(Symbol.EURUSD)).thenReturn(new BigDecimal("100"));
        when(priceGenerator.generatePrice(Symbol.AAPL)).thenReturn(new BigDecimal("10"));

        // usedMargin = (|2|*100 + |3|*10) * 0.1 = (200 + 30) * 0.1 = 233 * 0.1 = 23.0
        // requiredMargin = maxNotional * marginRate = 1000 * 0.1 = 100
        // availableMargin = equity - usedMargin = 1000 - 23 = 977 >= 100 -> success
        RiskService.RiskCheckResult ok = riskService.riskCheck(accountId, Symbol.EURUSD, BigDecimal.ZERO, Side.BUY);
        assertTrue(ok.allowed());

        // if equity were lower, e.g., 10, available < required and should throw
        AccountEntity poor = AccountEntity.builder()
            .id(accountId)
            .equity(new BigDecimal("10.00"))
            .createdAt(Instant.now())
            .build();
        when(accountsRepository.findById(accountId)).thenReturn(Optional.of(poor));

        RiskService.RiskCheckResult poorResult = riskService.riskCheck(accountId, Symbol.EURUSD, BigDecimal.ZERO, Side.BUY);
        assertFalse(poorResult.allowed());
        assertEquals(RejectionReason.REQUIRED_MARGIN.name(), poorResult.reason());
    }

    @Test
    void orderNotional_exceedsMaxNotional_throwsRiskCheckFailed() {
        AccountEntity account = AccountEntity.builder()
            .id(accountId)
            .equity(new BigDecimal("100000.00"))
            .createdAt(Instant.now())
            .build();

        // maxNotional set to 100
        AccountLimitEntity limit = AccountLimitEntity.builder()
            .accountId(accountId)
            .maxNotional(new BigDecimal("100"))
            .maxPosPerSymbol(new BigDecimal("1000"))
            .marginRateFx(new BigDecimal("0.1"))
            .marginRateStock(new BigDecimal("0.1"))
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        when(accountsRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accountLimitsRepository.findById(accountId)).thenReturn(Optional.of(limit));
        when(positionsRepository.findByIdAccountId(accountId)).thenReturn(List.of());

        // price 60, quantity 2 => orderNotional = 120 > 100 -> should throw
        when(priceGenerator.generatePrice(Symbol.AAPL)).thenReturn(new BigDecimal("60"));

        RiskService.RiskCheckResult result = riskService.riskCheck(accountId, Symbol.AAPL, new BigDecimal("2"), Side.BUY);
        assertFalse(result.allowed());
        assertEquals(RejectionReason.NOTIONAL_LIMIT.name(), result.reason());
    }

    @Test
    void orderNotional_equalToMaxNotional_allowsRiskCheck() {
        AccountEntity account = AccountEntity.builder()
            .id(accountId)
            .equity(new BigDecimal("100000.00"))
            .createdAt(Instant.now())
            .build();

        // maxNotional set to 100
        AccountLimitEntity limit = AccountLimitEntity.builder()
            .accountId(accountId)
            .maxNotional(new BigDecimal("100"))
            .maxPosPerSymbol(new BigDecimal("1000"))
            .marginRateFx(new BigDecimal("0.1"))
            .marginRateStock(new BigDecimal("0.1"))
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        when(accountsRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accountLimitsRepository.findById(accountId)).thenReturn(Optional.of(limit));
        when(positionsRepository.findByIdAccountId(accountId)).thenReturn(List.of());

        // price 50, quantity 2 => orderNotional = 100 == maxNotional -> should not throw
        when(priceGenerator.generatePrice(Symbol.AAPL)).thenReturn(new BigDecimal("50"));

        RiskService.RiskCheckResult result = riskService.riskCheck(accountId, Symbol.AAPL, new BigDecimal("2"), Side.BUY);
        assertTrue(result.allowed());
    }

    @Test
    void sell_moreThanPosition_rejected() {
        // existing position smaller than sell quantity
        PositionEntity pos = PositionEntity.builder()
            .id(new PositionId(accountId, Symbol.EURUSD))
            .netQty(new BigDecimal("2"))
            .avgPrice(new BigDecimal("100.00"))
            .build();

        when(positionsRepository.findByIdAccountId(accountId)).thenReturn(List.of(pos));

        RiskService.RiskCheckResult result = riskService.riskCheck(accountId, Symbol.EURUSD, new BigDecimal("5"), Side.SELL);
        assertFalse(result.allowed());
        assertEquals(RejectionReason.INSUFFICIENT_POSITION.name(), result.reason());
    }

    @Test
    void sell_withinPosition_allowsRiskCheck() {
        // existing position larger than sell quantity
        PositionEntity pos = PositionEntity.builder()
            .id(new PositionId(accountId, Symbol.EURUSD))
            .netQty(new BigDecimal("10"))
            .avgPrice(new BigDecimal("100.00"))
            .build();

        when(positionsRepository.findByIdAccountId(accountId)).thenReturn(List.of(pos));

        RiskService.RiskCheckResult result = riskService.riskCheck(accountId, Symbol.EURUSD, new BigDecimal("5"), Side.SELL);
        assertTrue(result.allowed());
    }


    @Test
    void testTtest() {
        //Sequential / Indexed Collections
        ArrayList<String> arrayList = new ArrayList<>(); //general purpose, iterating, not good for adding stuff in the middle
        LinkedList<String> linkedList = new LinkedList<>(); // adding head or tail a lot, want a queue or a stack. Prefer ArrayDequeue which does not use Nodes, thus less memory overhead
        int[] intArray = new int[5]; // primitive array, similar to array list
        CopyOnWriteArrayList<String> copyOnWriteArrayList = new CopyOnWriteArrayList<>();// THREAD SAFE, concurrent, multiple read concurrently, writes new array on write, but iterators iterate the previous snapshot before the write

        //FIFO, LIFO & Priority Access
        ArrayDeque<String> arrayDeque = new ArrayDeque<String>();// preffered over Stack and LinkedList, does not accept nulls. If you want stack or queue this is you dude
        Stack<String> stack = new Stack<>(); // legacy choice, not prefferred, uses Vector which exposes unnecessary methods
        PriorityQueue<Integer> priorityQueue = new PriorityQueue<>(); // essentially a min heap (but can change comparator to suit your needs)
        ArrayBlockingQueue<String> arrayBlockingQueue = new ArrayBlockingQueue<>(7);// THREAD SAFE Queue, bounded meaning specific size, threads can wait to add when full, controlling throughput
        LinkedBlockingQueue<String> linkedBlockingQueue = new LinkedBlockingQueue<>();// THREAD SAFE, It's a queue but only FIFO, can optionally proviude capacity, but it's pretty much the above without capacity
        LinkedBlockingDeque<String> stringLinkedBlockingDeque = new LinkedBlockingDeque<>(); // THREAD SAFE, but not necesssarily FIFO as it can add or remove from both ends
        ConcurrentLinkedQueue<String> concurrentLinkedQueue = new ConcurrentLinkedQueue<>(); // THREAD SAFE, uses CAS, good for high concurrency, so if you don't want to BLOCK like LinkedBlockingQueue use this
        SynchronousQueue<String> synchronousQueue = new SynchronousQueue<>();// THREAD SAFE, not really a queue but think of it as a handoff variable. When you want to put you must wait for another remove, and you wnat to remove, you must wait for a put
        DelayQueue<Delayed> delayQueue = new DelayQueue<>(); // THREAD SAFE, elements can only be taken when their delay has expired, useful for scheduling tasks in the future

        //Key → Value Lookups
        HashMap<String, String> map = new HashMap<>();// it's a map, fast read writes, no ordering
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<>();// Hashmap but maintains order of insertion or lookup, if yo ucare about order use this
        TreeMap<String, String> treeMap = new TreeMap<>(); // orders KEYS using a red-black tree, slower
        ConcurrentHashMap<String, String> concurrentHashMap = new ConcurrentHashMap<>(); // THREAD SAFE, concurrent hash map, uses CAS, no null keys or values
        ConcurrentSkipListMap<String, String> concurrentSkipListMap = new ConcurrentSkipListMap<>();// THREAD SAFE, it's a concurrent TreeMap
        EnumMap<TestEnum, String> enumMap = new EnumMap<>(TestEnum.class);// Hashmap for enums

        // Unique Element Collections
        HashSet<String> set = new HashSet<>(); // backed by a hash table, quick check for uniqueness
        TreeSet<String> treeSet = new TreeSet<>(); // uses red black tree to order, all elements unique
        LinkedHashSet<String> linkedHashSet = new LinkedHashSet<>();// unique + insertion order
        ConcurrentSkipListSet<String> concurrentSkipListSet = new ConcurrentSkipListSet<>(); // THREAD SAFE TreeSet but concurrent
        EnumSet<TestEnum> enumSet = EnumSet.allOf(TestEnum.class); // Set for enums, all of them in this case

        //Atomic & Lock-Free Structures
        AtomicInteger atomicInteger = new AtomicInteger(); // THREAD SAFE counter, uses
        AtomicLong atomicLong = new AtomicLong();// LongAdder, same as above but long
        LongAdder longAdder = new LongAdder(); // LongAdder, It's for the above, but in high contention, uses more space
        LongAccumulator longAccumulator = new LongAccumulator((x, y) -> x + y, 0); // THREAD SAFE, similar to LongAdder but allows you to specify the function for accumulation, not just addition
        AtomicReference<String> atomicReference = new AtomicReference(); // THREAD SAFE, allows you to atomically update a reference to an object, useful for implementing lock-free algorithms
        AtomicStampedReference<String> atomicStampedReference = new AtomicStampedReference<>(null, 0); // THREAD SAFE, similar to AtomicReference but also includes a stamp (version number) to help prevent ABA problem in concurrent algorithms
        StampedLock stampedLock = new StampedLock(); // THREAD SAFE, a lock that also returns a stamp, can be used for optimistic reads, where you can read without locking and then check the stamp to see if it was modified during the read, more performant than ReentrantLock when reads are more frequent than writes
        ReentrantLock reentrantLock = new ReentrantLock(); // THREAD SAFE, a traditional lock that can be re-entered by the same thread, supports fairness and condition variables

        //Niche but Powerful
        BitSet bitSet = new BitSet(); // a set of bits that can grow as needed, useful for memory-efficient storage of boolean values or for performing bitwise operations
        WeakHashMap<String, String> weakHashMap = new WeakHashMap<>(); // a HashMap that holds weak references to its keys, allowing them to be garbage collected when they are no longer in use
        IdentityHashMap<String, String> identityHashMap = new IdentityHashMap<>(); // a HashMap that uses reference equality (==) instead of object equality (.equals()) for its keys
    }

    public enum TestEnum {

    }
}

