use std::collections::HashMap;
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::{Arc, LazyLock, Mutex};

static REGISTRY: LazyLock<Mutex<HashMap<i64, Arc<AtomicBool>>>> =
    LazyLock::new(|| Mutex::new(HashMap::new()));

pub fn register(cancel_id: i64) -> Arc<AtomicBool> {
    let flag = Arc::new(AtomicBool::new(false));
    if cancel_id != 0 {
        REGISTRY.lock().unwrap().insert(cancel_id, flag.clone());
    }
    flag
}

pub fn signal(cancel_id: i64) {
    if let Some(flag) = REGISTRY.lock().unwrap().get(&cancel_id) {
        flag.store(true, Ordering::SeqCst);
    }
}

pub fn unregister(cancel_id: i64) {
    if cancel_id != 0 {
        REGISTRY.lock().unwrap().remove(&cancel_id);
    }
}
