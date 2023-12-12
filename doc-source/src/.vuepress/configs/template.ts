import { i18n } from './utils';

const navigationLinks = {
    start: [
        '/guide/home',
        '/guide/knowledge',
        '/guide/quick-start',
        '/guide/example',
        '/guide/performance-optimization.md',
        '/guide/structural-zoom-table',
        '/guide/run-on-desktop',
    ],
    about: [
        '/about/contacts',
        '/about/about'
    ]
};

export const configs = {
    dev: {
        dest: '../docs/',
        port: 8080
    },
    website: {
        base: '/DexKit/',
        icon: '/DexKit/images/logo.png',
        logo: '/images/logo.png',
        title: 'DexKit',
        locales: {
            '/en/': {
                lang: 'en-US',
                description: 'A high-performance dex runtime parsing library implemented in C++'
            },
            '/zh-cn/': {
                lang: 'zh-CN',
                description: '一个使用 C++ 实现的高性能运行时 dex 解析库'
            }
        }
    },
    github: {
        repo: 'https://github.com/LuckyPray/DexKit',
        branch: 'master',
        dir: 'doc-source/src'
    }
};

export const navBarItems = {
    '/en/': [{
        text: 'Navigation',
        children: [{
            text: 'Get Started',
            children: [
                { text: 'Introduce', link: i18n.string(navigationLinks.start[0], 'en') },
                { text: 'Basic Knowledge', link: i18n.string(navigationLinks.start[1], 'en') },
                { text: 'Quick Start', link: i18n.string(navigationLinks.start[2], 'en') },
                { text: 'Usage Example', link: i18n.string(navigationLinks.start[3], 'en') },
                { text: 'Performance optimization', link: i18n.string(navigationLinks.start[4], 'en') },
                { text: 'Structural Zoom Table', link: i18n.string(navigationLinks.start[5], 'en') },
                { text: 'Run on Desktop', link: i18n.string(navigationLinks.start[6], 'en') },
            ]
        }, {
            text: 'About',
            children: [
                { text: 'Contact Us', link: i18n.string(navigationLinks.about[0], 'zh-cn') },
                { text: 'About this Document', link: i18n.string(navigationLinks.about[1], 'zh-cn') }
            ]
        }]
    }],
    '/zh-cn/': [{
        text: '导航',
        children: [{
            text: '入门',
            children: [
                { text: '介绍', link: i18n.string(navigationLinks.start[0], 'zh-cn') },
                { text: '基础知识', link: i18n.string(navigationLinks.start[1], 'zh-cn') },
                { text: '快速开始', link: i18n.string(navigationLinks.start[2], 'zh-cn') },
                { text: '用法示例', link: i18n.string(navigationLinks.start[3], 'zh-cn') },
                { text: '性能优化', link: i18n.string(navigationLinks.start[4], 'zh-cn') },
                { text: '结构速查表', link: i18n.string(navigationLinks.start[5], 'zh-cn') },
                { text: '桌面平台运行', link: i18n.string(navigationLinks.start[6], 'zh-cn') },
            ]
        }, {
            text: '关于',
            children: [
                { text: '联系我们', link: i18n.string(navigationLinks.about[0], 'zh-cn') },
                { text: '关于此文档', link: i18n.string(navigationLinks.about[1], 'zh-cn') }
            ]
        }]
    }]
};

export const sideBarItems = {
    '/en/': [{
        text: 'Get Started',
        collapsible: true,
        children: i18n.array(navigationLinks.start, 'en')
    }, {
        text: 'About',
        collapsible: true,
        children: i18n.array(navigationLinks.about, 'en')
    }],
    '/zh-cn/': [{
        text: '入门',
        collapsible: true,
        children: i18n.array(navigationLinks.start, 'zh-cn')
    }, {
        text: '关于',
        collapsible: true,
        children: i18n.array(navigationLinks.about, 'zh-cn')
    }]
};
