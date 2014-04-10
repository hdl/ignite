/* @java.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.kernal.processors.cache.datastructures;

import junit.framework.*;
import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.cache.datastructures.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.kernal.processors.cache.query.*;
import org.gridgain.grid.util.lang.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.*;

import java.util.*;
import java.util.concurrent.*;

import static org.gridgain.grid.cache.GridCacheDistributionMode.*;
import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCachePreloadMode.*;
import static org.gridgain.grid.cache.GridCacheWriteSynchronizationMode.*;

/**
 * Cache set tests.
 */
public abstract class GridCacheSetAbstractSelfTest extends GridCacheAbstractSelfTest {
    /** */
    private static final String SET_NAME = "testSet";

    /** {@inheritDoc} */
    @Override protected int gridCount() {
        return 4;
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        waitSetResourcesCleared();

        cache().dataStructures().removeSet(SET_NAME);

        assertNull(cache().dataStructures().set(SET_NAME, false, false));

        super.afterTest();
    }

    /**
     * Waits when internal set maps are cleared.
     *
     * @throws GridException If failed.
     */
    @SuppressWarnings("ErrorNotRethrown")
    private void waitSetResourcesCleared() throws GridException {
        final int MAX_CHECK = 5;

        for (int i = 0; i < MAX_CHECK; i++) {
            try {
                assertSetResourcesCleared();

                return;
            }
            catch (AssertionFailedError e) {
                if (i == MAX_CHECK - 1)
                    throw e;

                log.info("Set resources not cleared, will wait more.");

                U.sleep(1000);
            }
        }
    }

    /**
     * Checks internal set maps are cleared.
     */
    private void assertSetResourcesCleared() {
        for (int i = 0; i < gridCount(); i++) {
            GridKernal grid = (GridKernal)grid(i);

            GridCacheQueryManager queries = grid.internalCache(null).context().queries();

            Map map = GridTestUtils.getFieldValue(queries, GridCacheQueryManager.class, "qryIters");

            for (Object obj : map.values())
                assertEquals("Iterators not removed for grid " + i, 0, ((Map)obj).size());

            /*
            map = GridTestUtils.getFieldValue(ds, "setsMap");

            assertEquals("Set not removed for grid " + i, 0, map.size());

            map = GridTestUtils.getFieldValue(ds, "setDataMap");

            assertEquals("Set data not removed for grid " + i, 0, map.size());
            */
        }
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        if (cacheMode() == PARTITIONED) {
            GridCacheConfiguration ccfg1 = cacheConfiguration(gridName);

            GridCacheConfiguration ccfg2 = cacheConfiguration(gridName);

            ccfg2.setName("noBackupsCache");
            ccfg2.setBackups(0);

            cfg.setCacheConfiguration(ccfg1, ccfg2);
        }

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected GridCacheConfiguration cacheConfiguration(String gridName) throws Exception {
        GridCacheConfiguration ccfg = super.cacheConfiguration(gridName);

        ccfg.setPreloadMode(SYNC);
        ccfg.setWriteSynchronizationMode(FULL_SYNC);

        return ccfg;
    }

    /** {@inheritDoc} */
    @Override protected GridCacheDistributionMode distributionMode() {
        return PARTITIONED_ONLY;
    }

    /** {@inheritDoc} */
    @Override protected long getTestTimeout() {
        return 2 * 60 * 1000;
    }

    /**
     * @throws Exception If failed.
     */
    public void testCreateRemove() throws Exception {
        testCreateRemove(false);
    }

    /**
     * @throws Exception If failed.
     */
    public void testCreateRemoveCollocated() throws Exception {
        testCreateRemove(true);
    }

    /**
     * @param collocated Collocation flag.
     * @throws Exception If failed.
     */
    private void testCreateRemove(boolean collocated) throws Exception {
        for (int i = 0; i < gridCount(); i++)
            assertNull(cache(i).dataStructures().set(SET_NAME, collocated, false));

        GridCacheSet<Integer> set0 = cache().dataStructures().set(SET_NAME, collocated, true);

        assertNotNull(set0);

        for (int i = 0; i < gridCount(); i++) {
            GridCacheSet<Integer> set = cache().dataStructures().set(SET_NAME, collocated, true);

            assertNotNull(set);
            assertTrue(set.isEmpty());
            assertEquals(0, set.size());

            assertEquals(SET_NAME, set.name());

            if (cacheMode() == PARTITIONED)
                assertEquals(collocated, set.collocated());
        }

        assertTrue(cache().dataStructures().removeSet(SET_NAME));

        for (int i = 0; i < gridCount(); i++) {
            assertNull(cache(i).dataStructures().set(SET_NAME, collocated, false));

            assertFalse(cache(i).dataStructures().removeSet(SET_NAME));
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testApi() throws Exception {
        testApi(false);
    }

    /**
     * @throws Exception If failed.
     */
    public void testApiCollocated() throws Exception {
        testApi(true);
    }

    /**
     * @param collocated Collocation flag.
     * @throws Exception If failed.
     */
    private void testApi(boolean collocated) throws Exception {
        assertNotNull(cache().dataStructures().set(SET_NAME, collocated, true));

        for (int i = 0; i < gridCount(); i++) {
            Set<Integer> set = cache(i).dataStructures().set(SET_NAME, collocated, false);

            assertFalse(set.contains(1));
            assertEquals(0, set.size());
            assertTrue(set.isEmpty());
        }

        // Add, isEmpty.

        assertTrue(cache().dataStructures().set(SET_NAME, collocated, false).add(1));

        for (int i = 0; i < gridCount(); i++) {
            assertEquals(0, cache(i).size());

            Set<Integer> set = cache(i).dataStructures().set(SET_NAME, collocated, false);

            assertEquals(1, set.size());
            assertFalse(set.isEmpty());
            assertTrue(set.contains(1));

            assertFalse(set.add(1));

            assertFalse(set.contains(100));
        }

        // Remove.

        assertTrue(cache().dataStructures().set(SET_NAME, collocated, true).remove(1));

        for (int i = 0; i < gridCount(); i++) {
            Set<Integer> set = cache(i).dataStructures().set(SET_NAME, collocated, false);

            assertEquals(0, set.size());
            assertTrue(set.isEmpty());

            assertFalse(set.contains(1));

            assertFalse(set.remove(1));
        }

        // Contains all.

        Collection<Integer> col1 = new ArrayList<>();
        Collection<Integer> col2 = new ArrayList<>();

        final int ITEMS = 100;

        for (int i = 0; i < ITEMS; i++) {
            assertTrue(cache(i % gridCount()).dataStructures().set(SET_NAME, collocated, false).add(i));

            col1.add(i);
            col2.add(i);
        }

        col2.add(ITEMS);

        for (int i = 0; i < gridCount(); i++) {
            Set<Integer> set = cache(i).dataStructures().set(SET_NAME, collocated, false);

            assertEquals(ITEMS, set.size());
            assertTrue(set.containsAll(col1));
            assertFalse(set.containsAll(col2));
        }

        // To array.

        for (int i = 0; i < gridCount(); i++) {
            Set<Integer> set = cache(i).dataStructures().set(SET_NAME, collocated, false);

            assertArrayContent(set.toArray(), ITEMS);
            assertArrayContent(set.toArray(new Integer[ITEMS]), ITEMS);
        }

        // Remove all.

        Collection<Integer> rmvCol = new ArrayList<>();

        for (int i = ITEMS - 10; i < ITEMS; i++)
            rmvCol.add(i);

        assertTrue(cache().dataStructures().set(SET_NAME, collocated, false).removeAll(rmvCol));

        for (int i = 0; i < gridCount(); i++) {
            Set<Integer> set = cache(i).dataStructures().set(SET_NAME, collocated, false);

            assertFalse(set.removeAll(rmvCol));

            for (Integer val : rmvCol)
                assertFalse(set.contains(val));

            assertArrayContent(set.toArray(), ITEMS - 10);
            assertArrayContent(set.toArray(new Integer[ITEMS - 10]), ITEMS - 10);
        }

        // Add all.

        assertTrue(cache().dataStructures().set(SET_NAME, collocated, false).addAll(rmvCol));

        for (int i = 0; i < gridCount(); i++) {
            Set<Integer> set = cache(i).dataStructures().set(SET_NAME, collocated, false);

            assertEquals(ITEMS, set.size());

            assertFalse(set.addAll(rmvCol));

            for (Integer val : rmvCol)
                assertTrue(set.contains(val));
        }

        // Retain all.

        assertTrue(cache().dataStructures().set(SET_NAME, collocated, false).retainAll(rmvCol));

        for (int i = 0; i < gridCount(); i++) {
            Set<Integer> set = cache(i).dataStructures().set(SET_NAME, collocated, false);

            assertEquals(rmvCol.size(), set.size());

            assertFalse(set.retainAll(rmvCol));

            for (int val = 0; val < 10; val++)
                assertFalse(set.contains(val));

            for (int val : rmvCol)
                assertTrue(set.contains(val));
        }

        // Clear.

        cache().dataStructures().set(SET_NAME, collocated, false).clear();

        for (int i = 0; i < gridCount(); i++) {
            Set<Integer> set = cache(i).dataStructures().set(SET_NAME, collocated, false);

            assertEquals(0, set.size());
            assertTrue(set.isEmpty());
            assertFalse(set.contains(0));
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testIterator() throws Exception {
        testIterator(false);
    }

    /**
     * @throws Exception If failed.
     */
    public void testIteratorCollocated() throws Exception {
        testIterator(true);
    }

    /**
     * @param collocated Collocation flag.
     * @throws Exception If failed.
     */
    @SuppressWarnings("deprecation")
    private void testIterator(boolean collocated) throws Exception {
        final GridCacheSet<Integer> set0 = cache().dataStructures().set(SET_NAME, collocated, true);

        GridTestUtils.assertThrows(log, new Callable<Void>() {
            @Override public Void call() throws Exception {
                set0.iterator();

                return null;
            }
        }, UnsupportedOperationException.class, null);

        for (int i = 0; i < gridCount(); i++) {
            GridCacheSet<Integer> set = cache(i).dataStructures().set(SET_NAME, collocated, false);

            assertFalse(set.iteratorEx().hasNext());
        }

        int cnt = 0;

        for (int i = 0; i < gridCount(); i++) {
            Set<Integer> set = cache(i).dataStructures().set(SET_NAME, collocated, false);

            for (int j = 0; j < 100; j++)
                assertTrue(set.add(cnt++));
        }

        for (int i = 0; i < gridCount(); i++) {
            GridCacheSet<Integer> set = cache(i).dataStructures().set(SET_NAME, collocated, false);

            assertSetContent(set, cnt);
        }

        // Try to do not use hasNext.

        Collection<Integer> data = new HashSet<>(cnt);

        GridCloseableIterator<Integer> iter = set0.iteratorEx();

        for (int i = 0; i < cnt; i++)
            assertTrue(data.add(iter.next()));

        assertFalse(iter.hasNext());

        assertEquals(cnt, data.size());

        for (int i = 0; i < cnt; i++)
            assertTrue(data.contains(i));

        // Iterator for empty set.

        set0.clear();

        for (int i = 0; i < gridCount(); i++) {
            GridCacheSet<Integer> set = cache(i).dataStructures().set(SET_NAME, collocated, false);

            assertFalse(set.iteratorEx().hasNext());
        }

        // Iterator.remove().

        for (int i = 0; i < 10; i++)
            assertTrue(set0.add(i));

        iter = set0.iteratorEx();

        while (iter.hasNext()) {
            Integer val = iter.next();

            if (val % 2 == 0)
                iter.remove();
        }

        for (int i = 0; i < gridCount(); i++) {
            Set<Integer> set = cache(i).dataStructures().set(SET_NAME, collocated, false);

            assertEquals(i % 2 != 0, set.contains(i));
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testIteratorClose() throws Exception {
        testIteratorClose(false);
    }

    /**
     * @throws Exception If failed.
     */
    public void testIteratorCloseCollocated() throws Exception {
        testIteratorClose(true);
    }

    /**
     * @param collocated Collocation flag.
     * @throws Exception If failed.
     */
    private void testIteratorClose(boolean collocated) throws Exception {
        final GridCacheSet<Integer> set0 = cache().dataStructures().set(SET_NAME, collocated, true);

        for (int i = 0; i < 500; i++)
            assertTrue(set0.add(i));

        final GridCloseableIterator<Integer> iter = set0.iteratorEx();

        assertFalse(iter.isClosed());

        assertTrue(iter.hasNext());

        iter.next();

        assertTrue(iter.hasNext());

        iter.close();

        GridTestUtils.assertThrows(log, new Callable<Void>() {
            @Override public Void call() throws Exception {
                iter.next();

                return null;
            }
        }, NoSuchElementException.class, null);

        assertTrue(iter.isClosed());
        assertFalse(iter.hasNext());
    }

    /**
     * TODO: GG-7952, enable when fixed.
     *
     * @throws Exception If failed.
     */
    public void _testNodeJoinsAndLeaves() throws Exception {
        testNodeJoinsAndLeaves(false);
    }

    /**
     * TODO: GG-7952, enable when fixed.
     *
     * @throws Exception If failed.
     */
    public void _testNodeJoinsAndLeavesCollocated() throws Exception {
        testNodeJoinsAndLeaves(true);
    }

    /**
     * @param collocated Collocation flag.
     * @throws Exception If failed.
     */
    private void testNodeJoinsAndLeaves(boolean collocated) throws Exception {
        if (cacheMode() == LOCAL)
            return;

        Set<Integer> set0 = cache().dataStructures().set(SET_NAME, collocated, true);

        final int ITEMS = 10_000;

        for (int i = 0; i < ITEMS; i++)
            set0.add(i);

        startGrid(gridCount());

        try {
            GridCacheSet<Integer> set1 = cache().dataStructures().set(SET_NAME, collocated, false);

            assertNotNull(set1);

            for (int i = 0; i < gridCount() + 1; i++) {
                GridCacheSet<Integer> set = cache(i).dataStructures().set(SET_NAME, collocated, false);

                assertEquals(ITEMS, set.size());

                assertSetContent(set, ITEMS);
            }
        }
        finally {
            stopGrid(gridCount());
        }

        for (int i = 0; i < gridCount(); i++) {
            GridCacheSet<Integer> set = cache(i).dataStructures().set(SET_NAME, collocated, false);

            assertSetContent(set, ITEMS);
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testCollocation() throws Exception {
        if (cacheMode() != PARTITIONED)
            return;

        final String setName = SET_NAME + "testCollocation";

        Set<Integer> set0 = grid(0).cache("noBackupsCache").dataStructures().set(setName, true, true);

        try {
            for (int i = 0; i < 1000; i++)
                assertTrue(set0.add(i));

            assertEquals(1000, set0.size());

            UUID setNodeId = null;

            for (int i = 0; i < gridCount(); i++) {
                GridKernal grid = (GridKernal)grid(i);

                Iterator<GridCacheEntryEx<Object, Object>> entries =
                    grid.context().cache().internalCache("noBackupsCache").map().allEntries0().iterator();

                if (entries.hasNext()) {
                    if (setNodeId == null)
                        setNodeId = grid.localNode().id();
                    else
                        fail("For collocated set all items should be stored on single node.");
                }
            }
        }
        finally {
            grid(0).cache("noBackupsCache").dataStructures().removeSet(setName);
        }
    }

    /**
     * @throws Exception If failed.
     */
    public void testMultithreaded() throws Exception {
        testMultithreaded(false);
    }

    /**
     * @throws Exception If failed.
     */
    public void testMultithreadedCollocated() throws Exception {
        if (cacheMode() != PARTITIONED)
            return;

        testMultithreaded(true);
    }

    /**
     * @param collocated Collocation flag.
     * @throws Exception If failed.
     */
    private void testMultithreaded(final boolean collocated) throws Exception {
        Set<Integer> set0 = cache().dataStructures().set(SET_NAME, collocated, true);

        assertNotNull(set0);

        Collection<GridFuture> futs = new ArrayList<>();

        final int THREADS_PER_NODE = 5;
        final int KEY_RANGE = 10_000;
        final int ITERATIONS = 3000;

        for (int i = 0; i < gridCount(); i++) {
            final int idx = i;

            futs.add(GridTestUtils.runMultiThreadedAsync(new Callable<Void>() {
                @Override public Void call() throws Exception {
                    GridCache cache = grid(idx).cache(null);

                    GridCacheSet<Integer> set = cache.dataStructures().set(SET_NAME, collocated, false);

                    assertNotNull(set);

                    ThreadLocalRandom rnd = ThreadLocalRandom.current();

                    for (int i = 0; i < ITERATIONS; i++) {
                        switch (rnd.nextInt(4)) {
                            case 0:
                                set.add(rnd.nextInt(KEY_RANGE));

                                break;

                            case 1:
                                set.remove(rnd.nextInt(KEY_RANGE));

                                break;

                            case 2:
                                set.contains(rnd.nextInt(KEY_RANGE));

                                break;

                            case 3:
                                Iterator<Integer> iter = set.iteratorEx();

                                while (iter.hasNext())
                                    assertNotNull(iter.next());

                                break;

                            default:
                                fail();
                        }

                        if ((i + 1) % 500 == 0)
                            log.info("Executed iterations: " + (i + 1));
                    }

                    return null;
                }
            }, THREADS_PER_NODE, "testSetMultithreaded"));
        }

        for (GridFuture fut : futs)
            fut.get();
    }


    /**
     * @throws Exception If failed.
     */
    public void _testCleanup() throws Exception {
        testCleanup(false);
    }

    /**
     * @throws Exception If failed.
     */
    public void _testCleanupCollocated() throws Exception {
        testCleanup(true);
    }

    /**
     * @param collocated Collocation flag.
     * @throws Exception If failed.
     */
    private void testCleanup(boolean collocated) throws Exception {
        Set<Integer> set0 = cache().dataStructures().set(SET_NAME, collocated, true);

        assertNotNull(set0);

        List<Integer> items = new ArrayList<>(10_000);

        for (int i = 0; i < 10_000; i++)
            items.add(i);

        set0.addAll(items);

        assertEquals(10_000, set0.size());

        assertTrue(grid(1).cache(null).dataStructures().removeSet(SET_NAME));

        assertTrue(GridTestUtils.waitForCondition(new PAX() {
            @SuppressWarnings("WhileLoopReplaceableByForEach")
            @Override public boolean applyx() {
                int cnt = 0;

                for (int i = 0; i < gridCount(); i++) {
                    Iterator<GridCacheEntryEx<Object, Object>> entries =
                        ((GridKernal)grid(i)).context().cache().internalCache().map().allEntries0().iterator();

                    while (entries.hasNext()) {
                        cnt++;

                        entries.next();
                    }
                }

                if (cnt > 0) {
                    log.info("Found more cache entries than expected, will wait: " + cnt);

                    return false;
                }

                return true;
            }
        }, 5000));
    }

    /**
     * @param set Set.
     * @param size Expected size.
     */
    private void assertSetContent(GridCacheSet<Integer> set, int size) {
        Collection<Integer> data = new HashSet<>(size);

        for (Integer val : set.iteratorEx())
            assertTrue(data.add(val));

        assertEquals(size, data.size());

        for (int val = 0; val < size; val++)
            assertTrue(data.contains(val));
    }

    /**
     * @param arr Array.
     * @param size Expected size.
     */
    private void assertArrayContent(Object[] arr, int size) {
        assertEquals(size, arr.length);

        for (int i = 0; i < size; i++) {
            boolean found = false;

            for (Object obj : arr) {
                if (obj.equals(i)) {
                    found = true;

                    break;
                }
            }

            assertTrue(found);
        }
    }
}
