use jni::EnvUnowned;
use jni::errors::ThrowRuntimeExAndDefault;
use jni::objects::{JObject, JObjectArray, JString};
use jni::sys::jboolean;

use crate::error::NativeError;
use crate::jni_progress::JniProgressCallback;
use crate::repository::{
    check_repository, create_snapshot, create_snapshot_with_progress, init_repository,
    repository_exists, restore_snapshot, validate_repository,
};

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_xayah_libnative_Rustic_nativeInitLogger<'local>(
    _unowned_env: EnvUnowned<'local>,
    _this: JObject<'local>,
) {
    android_logger::init_once(
        android_logger::Config::default().with_max_level(log::LevelFilter::Info),
    );
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_xayah_libnative_Rustic_nativeInitRepository<'local>(
    mut unowned_env: EnvUnowned<'local>,
    _this: JObject<'local>,
    repository_path: JString<'local>,
    password: JString<'local>,
    option_keys: JObjectArray<'local, JString<'local>>,
    option_values: JObjectArray<'local, JString<'local>>,
) {
    unowned_env
        .with_env(|env| -> Result<(), NativeError> {
            let options = string_pairs_to_map(env, &option_keys, &option_values)?;
            crate::repository::init_repository_with_options(
                &repository_path.to_string(),
                &password.to_string(),
                &options,
            )
            .map_err(NativeError::from)
        })
        .resolve::<ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_xayah_libnative_Rustic_nativeRepositoryExists<'local>(
    mut unowned_env: EnvUnowned<'local>,
    _this: JObject<'local>,
    repository_path: JString<'local>,
) -> jboolean {
    unowned_env
        .with_env(|_env| -> Result<jboolean, NativeError> {
            repository_exists(&repository_path.to_string())
                .map(jboolean::from)
                .map_err(NativeError::from)
        })
        .resolve::<ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_xayah_libnative_Rustic_nativeValidateRepository<'local>(
    mut unowned_env: EnvUnowned<'local>,
    _this: JObject<'local>,
    repository_path: JString<'local>,
    password: JString<'local>,
) {
    unowned_env
        .with_env(|_env| -> Result<(), NativeError> {
            validate_repository(&repository_path.to_string(), &password.to_string())
                .map_err(NativeError::from)
        })
        .resolve::<ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_xayah_libnative_Rustic_nativeCreateSnapshot<'local>(
    mut unowned_env: EnvUnowned<'local>,
    _this: JObject<'local>,
    repository_path: JString<'local>,
    password: JString<'local>,
    source_paths: JObjectArray<'local, JString<'local>>,
    tags: JObjectArray<'local, JString<'local>>,
    callback: JObject<'local>,
    cancel_id: jni::sys::jlong,
    option_keys: JObjectArray<'local, JString<'local>>,
    option_values: JObjectArray<'local, JString<'local>>,
) -> JString<'local> {
    unowned_env
        .with_env(|env| -> Result<JString<'local>, NativeError> {
            let source_paths = string_array_to_vec(env, &source_paths)?;
            let tags = string_array_to_vec(env, &tags)?;
            let repository_path = repository_path.to_string();
            let password = password.to_string();
            let options = string_pairs_to_map(env, &option_keys, &option_values)?;
            let snapshot_id = if callback.as_raw().is_null() {
                crate::repository::create_snapshot_with_cancel(
                    &repository_path,
                    &password,
                    &source_paths,
                    &tags,
                    cancel_id,
                    &options,
                )
                .map_err(NativeError::from)?
            } else {
                let vm = env.get_java_vm()?;
                let callback = env.new_global_ref(&callback)?;
                let callback = JniProgressCallback::new(env, vm, callback)?;
                crate::repository::create_snapshot_with_progress_and_cancel(
                    &repository_path,
                    &password,
                    &source_paths,
                    &tags,
                    callback,
                    cancel_id,
                    &options,
                )
                .map_err(NativeError::from)?
            };

            env.new_string(snapshot_id).map_err(NativeError::from)
        })
        .resolve::<ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_xayah_libnative_Rustic_nativeCancelBackup<'local>(
    _unowned_env: EnvUnowned<'local>,
    _this: JObject<'local>,
    cancel_id: jni::sys::jlong,
) {
    crate::repository::signal_cancel(cancel_id);
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_xayah_libnative_Rustic_nativeRestoreSnapshot<'local>(
    mut unowned_env: EnvUnowned<'local>,
    _this: JObject<'local>,
    repository_path: JString<'local>,
    password: JString<'local>,
    snapshot_id: JString<'local>,
    destination_path: JString<'local>,
) {
    unowned_env
        .with_env(|_env| -> Result<(), NativeError> {
            restore_snapshot(
                &repository_path.to_string(),
                &password.to_string(),
                &snapshot_id.to_string(),
                &destination_path.to_string(),
            )
            .map_err(NativeError::from)
        })
        .resolve::<ThrowRuntimeExAndDefault>()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_xayah_libnative_Rustic_nativeCheckRepository<'local>(
    mut unowned_env: EnvUnowned<'local>,
    _this: JObject<'local>,
    repository_path: JString<'local>,
    password: JString<'local>,
) {
    unowned_env
        .with_env(|_env| -> Result<(), NativeError> {
            check_repository(&repository_path.to_string(), &password.to_string())
                .map_err(NativeError::from)
        })
        .resolve::<ThrowRuntimeExAndDefault>()
}

fn string_pairs_to_map<'local>(
    env: &mut jni::Env<'local>,
    keys: &JObjectArray<'local, JString<'local>>,
    values: &JObjectArray<'local, JString<'local>>,
) -> Result<std::collections::BTreeMap<String, String>, NativeError> {
    let mut map = std::collections::BTreeMap::new();
    let len = keys.len(env)?;
    for index in 0..len {
        let key: JString<'local> = keys.get_element(env, index)?;
        let value: JString<'local> = values.get_element(env, index)?;
        map.insert(key.to_string(), value.to_string());
    }
    Ok(map)
}

fn string_array_to_vec<'local>(
    env: &mut jni::Env<'local>,
    array: &JObjectArray<'local, JString<'local>>,
) -> Result<Vec<String>, NativeError> {
    (0..array.len(env)?)
        .map(|index| {
            let value: JString<'local> = array.get_element(env, index)?;
            Ok(value.to_string())
        })
        .collect()
}
