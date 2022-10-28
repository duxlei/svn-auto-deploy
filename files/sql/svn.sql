create database if not exists svn_auto_deploy charset utf8mb4 collate utf8mb4_general_ci;
use svn_auto_deploy;

-- 发布任务主表
create table if not exists task_record (
    id              bigint          primary key auto_increment not null comment '主键',
    jira_no         varchar(20)     not null comment 'JIRA号',
    demand_name     varchar(500)    not null comment '需求名称',
    demand_type     tinyint         not null default 1 comment '需求类型 1-开发(需求)',
    relate_demand   varchar(500)    default '' comment '关联业务需求',
    principal       varchar(50)     not null comment '负责人',
    remark          varchar(500)    default '' comment '备注',
    iterate_week    date            not null comment '需求迭代周',
    out_dlls        varchar(5000)   not null comment '输出DLL（多个文件以应为逗号隔开）',
    status          tinyint         not null default 0 comment '最近发布状态 0-未发布1-已合并2-已编译3-发布成功4-发布失败',
    env             varchar(20)     not null comment '任务所属环境',

    create_time     datetime        not null default current_timestamp comment '创建时间',
    create_by       varchar(50)     not null comment '创建人',
    update_time     datetime        on update current_timestamp comment '任务所属环境',
    update_by       varchar(50)     default '' comment '更新人',
    deleted         tinyint         not null default 0 comment '删除标识',

    index idx_env_jra_no(env, jira_no),
    index idx_env_iterate_week(env, iterate_week),
    index idx_env_create_time(env, create_time)
) comment '发布任务主表';

-- 发布日志表
create table if not exists task_log (
    id              bigint          primary key auto_increment not null comment '主键',
    task_id         bigint          not null comment '发布任务ID',
    task_log        text            not null comment '发布log日志',
    status          tinyint         not null default 0 comment '发布状态 0-未发布1-已合并2-已编译3-发布成功4-发布失败',

    create_time     datetime        not null default current_timestamp comment '创建时间',
    create_by       varchar(50)     not null comment '创建人',
    update_time     datetime        on update current_timestamp comment '任务所属环境',
    update_by       varchar(50)     default '' comment '更新人',
    deleted         tinyint         not null default 0 comment '删除标识',

    index idx_env_jra_no(task_id)
) comment '发布日志表';