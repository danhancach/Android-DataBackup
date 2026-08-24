use std::collections::BTreeMap;
use std::sync::Arc;
use std::sync::atomic::AtomicBool;

use rustic_backend::BackendOptions;
use rustic_core::{
    BackupOptions, CheckOptions, ConfigOptions, Credentials, KeyOptions, LocalDestination,
    LsOptions, OpenStatus, PathList, Repository, RepositoryBackends, RepositoryOptions,
    RestoreOptions, SnapshotOptions,
};

use crate::Result;
use crate::progress::{AndroidProgressBars, RusticProgressCallback};

pub fn init_repository(repository_path: &str, password: &str) -> Result<()> {
    init_repository_with_options(repository_path, password, &BTreeMap::new())
}

pub fn init_repository_with_options(
    repository_path: &str,
    password: &str,
    options: &BTreeMap<String, String>,
) -> Result<()> {
    let credentials = Credentials::password(password);

    Repository::new(
        &RepositoryOptions::default(),
        &backends(repository_path, options, no_cancel())?,
    )?
    .init(
        &credentials,
        &KeyOptions::default(),
        &ConfigOptions::default(),
    )?;

    Ok(())
}

pub fn repository_exists(repository_path: &str) -> Result<bool> {
    repository_exists_with_options(repository_path, &BTreeMap::new())
}

pub fn repository_exists_with_options(
    repository_path: &str,
    options: &BTreeMap<String, String>,
) -> Result<bool> {
    let repo = Repository::new(
        &RepositoryOptions::default(),
        &backends(repository_path, options, no_cancel())?,
    )?;

    Ok(repo.config_id()?.is_some())
}

pub fn validate_repository(repository_path: &str, password: &str) -> Result<()> {
    validate_repository_with_options(repository_path, password, &BTreeMap::new())
}

pub fn validate_repository_with_options(
    repository_path: &str,
    password: &str,
    options: &BTreeMap<String, String>,
) -> Result<()> {
    open_repository(repository_path, password, options)?;

    Ok(())
}

pub fn create_snapshot(
    repository_path: &str,
    password: &str,
    source_paths: &[String],
    tags: &[String],
) -> Result<String> {
    create_snapshot_with_cancel(repository_path, password, source_paths, tags, 0, &BTreeMap::new())
}

pub fn create_snapshot_with_cancel(
    repository_path: &str,
    password: &str,
    source_paths: &[String],
    tags: &[String],
    cancel_id: i64,
    options: &BTreeMap<String, String>,
) -> Result<String> {
    let cancel = crate::cancel::register(cancel_id);
    let result = create_snapshot_from_repository(
        open_repository(repository_path, password, options)?,
        source_paths,
        tags,
        cancel.clone(),
    );
    crate::cancel::unregister(cancel_id);
    result
}

pub fn create_snapshot_with_progress<C: RusticProgressCallback>(
    repository_path: &str,
    password: &str,
    source_paths: &[String],
    tags: &[String],
    callback: C,
) -> Result<String> {
    create_snapshot_with_progress_and_cancel(
        repository_path,
        password,
        source_paths,
        tags,
        callback,
        0,
        &BTreeMap::new(),
    )
}

pub fn create_snapshot_with_progress_and_cancel<C: RusticProgressCallback>(
    repository_path: &str,
    password: &str,
    source_paths: &[String],
    tags: &[String],
    callback: C,
    cancel_id: i64,
    options: &BTreeMap<String, String>,
) -> Result<String> {
    let cancel = crate::cancel::register(cancel_id);
    let result = create_snapshot_from_repository(
        open_repository_with_progress(repository_path, password, options, callback)?,
        source_paths,
        tags,
        cancel.clone(),
    );
    crate::cancel::unregister(cancel_id);
    result
}

fn create_snapshot_from_repository(
    repo: Repository<OpenStatus>,
    source_paths: &[String],
    tags: &[String],
    cancel: Arc<AtomicBool>,
) -> Result<String> {
    let repo = repo.to_indexed_ids()?;
    let source = source_paths
        .iter()
        .map(std::path::PathBuf::from)
        .collect::<PathList>()
        .sanitize()?;
    let snapshot_options = tags
        .iter()
        .try_fold(SnapshotOptions::default(), |options, tag| {
            options.add_tags(tag)
        })?;

    match repo.backup(
        &BackupOptions::default(),
        &source,
        snapshot_options.to_snapshot()?,
    ) {
        Ok(snapshot) => Ok(snapshot.id.to_string()),
        Err(error) => {
            if cancel.load(std::sync::atomic::Ordering::SeqCst) {
                Err("The backup was cancelled by the user.".into())
            } else {
                Err(error.into())
            }
        }
    }
}

pub fn restore_snapshot(
    repository_path: &str,
    password: &str,
    snapshot_id: &str,
    destination_path: &str,
) -> Result<()> {
    let repo = open_repository(repository_path, password, &BTreeMap::new())?.to_indexed()?;
    let node = repo.node_from_snapshot_path(snapshot_id, |_| true)?;
    let ls_options = LsOptions::default();
    let nodes = repo.ls(&node, &ls_options)?;
    let destination = LocalDestination::new(destination_path, true, !node.is_dir())?;
    let restore_options = RestoreOptions::default();
    let restore_plan =
        repo.prepare_restore(&restore_options, nodes.clone(), &destination, false)?;

    repo.restore(restore_plan, &restore_options, nodes, &destination)?;

    Ok(())
}

pub fn list_snapshots(
    repository_path: &str,
    password: &str,
    tag_filter: Option<&str>,
) -> Result<String> {
    let repo = open_repository(repository_path, password, &BTreeMap::new())?.to_indexed_ids()?;
    let mut snapshots = repo.get_all_snapshots()?;
    snapshots.sort_unstable_by(|left, right| right.time.cmp(&left.time));

    let mut entries = Vec::new();
    for snapshot in snapshots {
        let tags: Vec<String> = snapshot.tags.iter().map(ToString::to_string).collect();
        if let Some(filter) = tag_filter {
            if !tags.iter().any(|tag| tag == filter) {
                continue;
            }
        }
        let tags_json = tags
            .iter()
            .map(|tag| format!("\"{}\"", escape_json(tag)))
            .collect::<Vec<_>>()
            .join(",");
        let time_millis: i64 = snapshot
            .time
            .timestamp()
            .as_millisecond()
            .try_into()
            .unwrap_or(i64::MAX);
        entries.push(format!(
            "{{\"id\":\"{}\",\"time\":{},\"tags\":[{}]}}",
            snapshot.id, time_millis, tags_json
        ));
    }

    Ok(format!("[{}]", entries.join(",")))
}

fn escape_json(value: &str) -> String {
    value
        .replace('\\', "\\\\")
        .replace('"', "\\\"")
        .replace('\n', "\\n")
        .replace('\r', "\\r")
}

pub fn check_repository(repository_path: &str, password: &str) -> Result<()> {
    let repo = open_repository(repository_path, password, &BTreeMap::new())?;

    repo.check(CheckOptions::default().trust_cache(true))?;

    Ok(())
}

fn open_repository(
    repository_path: &str,
    password: &str,
    options: &BTreeMap<String, String>,
) -> Result<Repository<OpenStatus>> {
    Ok(
        Repository::new(
            &RepositoryOptions::default(),
            &backends(repository_path, options, no_cancel())?,
        )?
        .open(&Credentials::password(password))?,
    )
}

fn open_repository_with_progress<C: RusticProgressCallback>(
    repository_path: &str,
    password: &str,
    options: &BTreeMap<String, String>,
    callback: C,
) -> Result<Repository<OpenStatus>> {
    Ok(Repository::new_with_progress(
        &RepositoryOptions::default(),
        &backends(repository_path, options, no_cancel())?,
        AndroidProgressBars::new(callback),
    )?
    .open(&Credentials::password(password))?)
}

fn backends(
    repository_path: &str,
    options: &BTreeMap<String, String>,
    cancel: Arc<AtomicBool>,
) -> Result<RepositoryBackends> {
    let backends = BackendOptions::default()
        .repository(repository_path)
        .options(options.clone())
        .to_backends()?;
    Ok(crate::cancel_backend::wrap_write_backends(backends, cancel))
}

fn no_cancel() -> Arc<AtomicBool> {
    Arc::new(AtomicBool::new(false))
}

pub fn signal_cancel(cancel_id: i64) {
    crate::cancel::signal(cancel_id);
}
