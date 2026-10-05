import { useId } from "react";

// A lightweight product illustration, drawn locally without external assets.
export function SolarScene() {
  const id = useId().replace(/:/g, "");
  return (
    <div className="solar-scene" aria-hidden="true">
      <svg viewBox="0 0 580 390" fill="none">
        <defs>
          <linearGradient id={`${id}-panel`} x1="0" y1="0" x2="1" y2="1">
            <stop stopColor="#244f85" />
            <stop offset="0.5" stopColor="#103866" />
            <stop offset="1" stopColor="#081b36" />
          </linearGradient>
          <linearGradient id={`${id}-metal`} x1="0" y1="0" x2="1" y2="1">
            <stop stopColor="#fff" />
            <stop offset="0.5" stopColor="#e8ebef" />
            <stop offset="1" stopColor="#bfc7d1" />
          </linearGradient>
          <radialGradient id={`${id}-sun`}>
            <stop stopColor="#fffdf1" />
            <stop offset="0.65" stopColor="#ffebbc" />
            <stop offset="1" stopColor="#ffcf75" />
          </radialGradient>
          <filter
            id={`${id}-shadow`}
            x="-50%"
            y="-50%"
            width="200%"
            height="200%"
          >
            <feGaussianBlur stdDeviation="12" />
          </filter>
        </defs>
        <ellipse
          cx="302"
          cy="330"
          rx="166"
          ry="19"
          fill="#8296ad"
          opacity="0.18"
          filter={`url(#${id}-shadow)`}
        />
        <circle cx="416" cy="96" r="52" fill={`url(#${id}-sun)`} />
        <ellipse cx="285" cy="305" rx="191" ry="40" fill="#dfe5ec" />
        <ellipse
          cx="285"
          cy="296"
          rx="191"
          ry="40"
          fill={`url(#${id}-metal)`}
        />
        <path d="M217 249v45l15 3v-50M360 211v78l15-4v-75" fill="#aab6c3" />
        <path d="M128 192 321 116 442 213 249 294Z" fill="#8b9aae" />
        <path
          d="M128 184 321 108 442 205 249 286Z"
          fill={`url(#${id}-metal)`}
        />
        <g transform="matrix(0.965 -0.38 0.605 0.485 138 184)">
          <rect width="189" height="184" rx="3" fill={`url(#${id}-panel)`} />
          {Array.from({ length: 6 }, (_, col) =>
            Array.from({ length: 6 }, (_, row) => (
              <rect
                key={`${col}-${row}`}
                x={3 + col * 31}
                y={3 + row * 30}
                width="28"
                height="27"
                rx="2"
                fill="#5d96ce"
                fillOpacity={0.04 + (5 - row) * 0.017}
                stroke="#8bc5f5"
                strokeOpacity="0.32"
                strokeWidth="0.55"
              />
            )),
          )}
          <path
            d="M0 10h189M0 174h189M94 0v184"
            stroke="#ceddec"
            strokeOpacity="0.6"
          />
          <path d="M0 0h110L20 184H0Z" fill="white" opacity="0.035" />
        </g>
        <path
          d="M382 260c29 7 44 1 54-14"
          stroke="#9cacbd"
          strokeWidth="3"
          strokeLinecap="round"
        />
        <rect
          x="432"
          y="204"
          width="42"
          height="61"
          rx="9"
          fill={`url(#${id}-metal)`}
        />
        <rect x="442" y="217" width="22" height="32" rx="5" fill="#fff" />
        <path d="m455 223-8 12h6l-3 9 10-13h-7l2-8Z" fill="#0071e3" />
        <circle cx="454" cy="256" r="2" fill="#228b5b" />
      </svg>
    </div>
  );
}
